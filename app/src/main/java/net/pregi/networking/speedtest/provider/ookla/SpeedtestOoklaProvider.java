package net.pregi.networking.speedtest.provider.ookla;

import net.pregi.math.StatisticsReportDouble;
import net.pregi.math.StatisticsReportDoubleWithMedian;
import net.pregi.networking.speedtest.NetworkTesting;
import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.OnIOProgressListener;
import net.pregi.networking.speedtest.OnSpeedtestListener;
import net.pregi.networking.speedtest.ServerEntry;
import net.pregi.networking.speedtest.SocketUtil;
import net.pregi.networking.speedtest.TransferMeasure;
import net.pregi.networking.speedtest.provider.OnSpeedtestListenerResultToIOProgressWrapper;
import net.pregi.networking.speedtest.provider.SpeedtestProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpeedtestOoklaProvider extends SpeedtestProvider {
    private static Charset ISO_8859_1 = Charset.forName("ISO_8859_1");

    private X509TrustManager trustManager;
    private X509TrustManager getTrustManager() {
        return trustManager;
    }

    private final Object getSocketFactoryLock = new Object();
    private SSLSocketFactory socketFactory;
    private SSLSocketFactory getSocketFactory() {
        if (socketFactory == null) {
            synchronized (getSocketFactoryLock) {
                if (socketFactory == null) {
                    trustManager = SocketUtil.createTrustManager();
                    SSLContext sslContext = SocketUtil.createSSLContext(trustManager);
                    socketFactory = sslContext.getSocketFactory();
                }
            }
        }

        return socketFactory;
    }

    private Socket openPlainSocket(ServerEntry serverEntry) throws IOException {
        return new Socket(serverEntry.getHost(), serverEntry.getPort());
    }

    private SSLSocket openSSLSocket(ServerEntry serverEntry) throws IOException {
        SSLSocket s = (SSLSocket)getSocketFactory().createSocket(serverEntry.getHost(), serverEntry.getPort());
        s.startHandshake();
        return s;
    }

    /** <p>Prepares the bytes for a request header. Does not include the final CRLF that should end a header.</p> */
    private static byte[] prepareRequest(String type, HttpUrl uri) {
        StringBuilder request = new StringBuilder();
        String query = uri.encodedQuery();

        request.append(type);
        request.append(' ');
        request.append(uri.encodedPath());
        if (query != null) {
            request.append('?');
            request.append(query);
        }
        request.append(" HTTP/1.1\r\n");

        request.append("Host: ");
        request.append(uri.host());
        request.append(':');
        request.append(uri.port());
        request.append("\r\n");

        //request.append("User-Agent: Java/1.7\r\n");
        //request.append("Accept-Language: en-US,en\r\n");
        return request.toString().getBytes(ISO_8859_1);
    }

    private static HttpUrl.Builder toURIBuilder(String guid, ServerEntry serverEntry, boolean useHttps, String resource) {
        return new HttpUrl.Builder()
                .scheme(useHttps ? "https" : "http")
                .host(serverEntry.getHost())
                .port(serverEntry.getPort())
                .encodedPath((resource.length()>=1 && resource.charAt(0) == '/' ? "" : "/")+resource)
                .addQueryParameter("guid", guid);
    }

    /** <p> Do a single PING test. A socket is created and closed per invocation.</p>
     *
     * <p>This isn't really a true PING test:
     * 		the measured time returned is the time it takes to receive a typically small response
     * 		after sending the closing \r\n
     * 			(which concludes a body-less HTTP request) and flushing.</p>
     *
     * @return
     * @throws IOException
     */
    private TransferMeasure doPing(String guid, ServerEntry serverEntry, boolean useHttps) throws IOException {
        // Get target URI
        HttpUrl uri = toURIBuilder(guid, serverEntry, useHttps, "hello").build();

        // Connect socket
        Socket s = useHttps ? openSSLSocket(serverEntry) : openPlainSocket(serverEntry);

        try {
            return NetworkTesting.doHTTPPing(s, prepareRequest("GET", uri));
        } finally {
            s.close();
        }
    }

    private TransferMeasure doDownload(String guid, ServerEntry serverEntry, boolean useHttps, long size, OnIOProgressListener listener) throws IOException {
        // The process is pretty much similar to doPing.
        // However, we're measuring different points of the process.

        // Get target URI
        HttpUrl uri = toURIBuilder(guid, serverEntry, useHttps, "download")
                .addQueryParameter("size", Long.toString(size))
                .build();

        System.out.println("URI: "+uri.toString());

        // Connect socket
        Socket s = useHttps ? openSSLSocket(serverEntry) : openPlainSocket(serverEntry);

        try {
            return NetworkTesting.doHTTPDownload(s, prepareRequest("GET", uri), listener);
        } finally {
            s.close();
        }
    }

    private TransferMeasure doUpload(String guid, ServerEntry serverEntry, boolean useHttps, byte[] data, long size, OnIOProgressListener listener) throws IOException {
        // The meticulousness is less than that of doDownload,
        //		since we have more control over what is uploaded than downloadd.

        // Get target URI
        HttpUrl uri = toURIBuilder(guid, serverEntry, useHttps, "upload").build();

        // Connect socket
        Socket s = useHttps ? openSSLSocket(serverEntry) : openPlainSocket(serverEntry);

        try {
            return NetworkTesting.doHTTPUpload(s, prepareRequest("POST", uri), data, size, listener);
        } finally {
            s.close();
        }
    }

    private TransferMeasure doUploadRandomBytes(String guid, ServerEntry serverEntry, boolean useHttps, long size, OnIOProgressListener listener) throws IOException {
        byte[] data = new byte[(int)Math.min(size, 0x80000)];
        Random rng = new Random();

        for (int i=0,l=data.length;i<l;i++) {
            data[i] = (byte)(' '+rng.nextInt('~'-' '+1));
        }

        return doUpload(guid, serverEntry, useHttps, data, size, listener);
    }

    @Override
    public List<ServerEntry> downloadServerList() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(getSocketFactory(), getTrustManager())
                .build();

        List<ServerEntry> servers = new ArrayList<ServerEntry>();
        {
            // Get server availability.
            Request request = new Request.Builder()
                    .url("https://www.speedtest.net/api/js/servers?engine=js&https_functional=1")
                    .build();

            Response response = null;
            try {
                response = client.newCall(request).execute();

                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                BufferedReader rd = new BufferedReader(new InputStreamReader(response.body().byteStream()));

                ObjectMapper mapper = new ObjectMapper();
                ArrayNode serverList = (ArrayNode)mapper.reader().readTree(rd);

                for (int i=0, l=serverList.size(); i<l; i++) {
                    OoklaServerEntry serverEntry = new OoklaServerEntry((ObjectNode)serverList.get(i));
                    servers.add(serverEntry);
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }

        return servers;
    }

    @Override
    public void runTest(ServerEntry selectedServer, NetworkTestingOptions options, OnSpeedtestListener onSpeedtestListener) {
        onSpeedtestListener = new OnSpeedtestListenerResultToIOProgressWrapper(onSpeedtestListener);
        options = new NetworkTestingOptions(options); // Copy settings to avoid mid-run tampering.
        String guid =  UUID.randomUUID().toString();

        if (selectedServer != null) {
            onSpeedtestListener.onTestStart(selectedServer.getHost(), selectedServer.getPort(), options);
            // Let's do some testing!

            // Ping
            if (!onSpeedtestListener.isRequestingTestStop()) {
                int count = options.getPingCount();

                if (count > 0) {
                    onSpeedtestListener.onPingStart(options.getPingCount());
                    List<TransferMeasure> results = new ArrayList<TransferMeasure>();
                    StatisticsReportDouble runningStats = new StatisticsReportDouble();
                    int failCount = 0;
                    for (int i = 0; !onSpeedtestListener.isRequestingTestStop() && i < count; i++) {
                        try {
                            TransferMeasure result = doPing(guid, selectedServer, options.getUseHttps());
                            results.add(result);
                            runningStats.addValue(result.getNanoseconds() / 1000000.0);
                            onSpeedtestListener.onPingResult(result, result.getMilliseconds(), runningStats);
                        } catch (Exception e) {
                            failCount++;
                            onSpeedtestListener.onPingException(e);
                            if (!(e instanceof IOException)) {
                                e.printStackTrace();
                            }
                        }
                    }


                    if (results.size() > 0) {
                        // backwards compatibility
                        double[] sampleMilliseconds = new double[results.size()];
                        int i = 0;
                        for (TransferMeasure m : results) {
                            sampleMilliseconds[i] = m.getNanoseconds() / 1000000.0;
                            i++;
                        }
                        StatisticsReportDoubleWithMedian stats = new StatisticsReportDoubleWithMedian(sampleMilliseconds);

                        onSpeedtestListener.onPingEnd(results, failCount, stats);
                    } else {
                        StatisticsReportDouble nullStats = new StatisticsReportDouble();

                        onSpeedtestListener.onPingEnd(results, failCount, nullStats);
                    }
                }
            } else {
                onSpeedtestListener.onTestInterrupted(null);
                return;
            }

            if (!onSpeedtestListener.isRequestingTestStop()) {
                int count = options.getDownloadCount();

                if (count > 0) {
                    onSpeedtestListener.onDownloadStart(count);

                    List<TransferMeasure> results = new ArrayList<TransferMeasure>();
                    int failCount = 0;
                    long size = options.getDownloadSize();
                    StatisticsReportDouble runningStats = new StatisticsReportDouble();
                    for (int i = 0; !onSpeedtestListener.isRequestingTestStop() && i < count; i++) {
                        try {
                            TransferMeasure result = doDownload(guid, selectedServer, options.getUseHttps(), size, onSpeedtestListener);
                            if (result != null) {
                                results.add(result);
                                runningStats.addValue(result.getByteCount() * 1000f / result.getMilliseconds());

                                onSpeedtestListener.onDownloadResult(result, result.getByteCount(), result.getMilliseconds() / 1000f, runningStats);
                            } else {
                                failCount++;
                                onSpeedtestListener.onDownloadResult(null, 0, 0, runningStats);
                            }
                        } catch (Exception e) {
                            failCount++;
                            onSpeedtestListener.onDownloadException(e);
                            if (!(e instanceof IOException)) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (results.size() > 0) {
                        // backwards compatibility
                        double[] sampleBytesPerSecond = new double[results.size()];

                        int i = 0;
                        for (TransferMeasure m : results) {
                            sampleBytesPerSecond[i] = m.getByteCount() / (m.getNanoseconds() / 1000000000.0);
                            i++;
                        }
                        StatisticsReportDoubleWithMedian stats = new StatisticsReportDoubleWithMedian(sampleBytesPerSecond);

                        onSpeedtestListener.onDownloadEnd(results, failCount, stats);
                    } else {
                        StatisticsReportDouble nullStats = new StatisticsReportDouble();

                        onSpeedtestListener.onDownloadEnd(results, failCount, nullStats);
                    }
                }
            } else {
                onSpeedtestListener.onTestInterrupted(null);
                return;
            }

            if (!onSpeedtestListener.isRequestingTestStop()) {
                int count = options.getUploadCount();

                if (count > 0) {
                    onSpeedtestListener.onUploadStart(count);

                    List<TransferMeasure> results = new ArrayList<TransferMeasure>();
                    int failCount = 0;
                    long size = options.getUploadSize();
                    StatisticsReportDouble runningStats = new StatisticsReportDouble();
                    for (int i = 0; !onSpeedtestListener.isRequestingTestStop() && i < count; i++) {
                        try {
                            TransferMeasure result = doUploadRandomBytes(guid, selectedServer, options.getUseHttps(), size, onSpeedtestListener);
                            results.add(result);
                            runningStats.addValue(result.getByteCount() * 1000f / result.getMilliseconds());

                            onSpeedtestListener.onUploadResult(result, result.getByteCount(), result.getMilliseconds() / 1000f, runningStats);
                        } catch (Exception e) {
                            failCount++;
                            onSpeedtestListener.onUploadException(e);
                            if (!(e instanceof IOException)) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (results.size() > 0) {
                        // backwards compatibility
                        double[] sampleBytesPerSecond = new double[results.size()];

                        int i = 0;
                        for (TransferMeasure m : results) {
                            sampleBytesPerSecond[i] = m.getByteCount() / (m.getNanoseconds() / 1000000000.0);
                            i++;
                        }
                        StatisticsReportDoubleWithMedian stats = new StatisticsReportDoubleWithMedian(sampleBytesPerSecond);

                        onSpeedtestListener.onUploadEnd(results, failCount, stats);
                    } else {
                        StatisticsReportDouble nullStats = new StatisticsReportDouble();

                        onSpeedtestListener.onUploadEnd(results, failCount, nullStats);
                    }
                }
            } else {
                onSpeedtestListener.onTestInterrupted(null);
                return;
            }
            onSpeedtestListener.onTestEnd();
        }
    }

    public SpeedtestOoklaProvider() {
        setProviderName("Ookla");
    }
}
