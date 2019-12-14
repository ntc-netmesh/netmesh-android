package net.pregi.networking.speedtest.provider.asti;

import net.pregi.math.StatisticsReportDouble;
import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.OnSpeedtestListener;
import net.pregi.networking.speedtest.ServerEntry;
import net.pregi.networking.speedtest.TransferMeasure;
import net.pregi.networking.speedtest.provider.OnSpeedtestListenerResultToIOProgressWrapper;
import net.pregi.networking.speedtest.provider.SpeedtestProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;

public class SpeedtestASTISagoGulamanProvider extends SpeedtestProvider {
    @Override
    public List<ServerEntry> downloadServerList() throws IOException {
        List<ServerEntry> entries = new ArrayList<ServerEntry>();

        ServerEntry e = new ServerEntry(1,
                "ASTI-COARE Web-based Speedtest Server",
                "DOST-ASTI",
                "Philippines",
                "PH",
                "speedtest.netmesh.xyz",
                -1,
                "https://speedtest.netmesh.xyz/speedtest");
        entries.add(e);

        return entries;
    }

    private static final int TESTPHASE_NOT_STARTED = 0;
    private static final int TESTPHASE_PING = 1;
    private static final int TESTPHASE_DOWNLOAD = 2;
    private static final int TESTPHASE_UPLOAD = 3;
    private static final int TESTPHASE_END = 100;

    private static class Vars {
        NetworkTestingOptions actualOptions;
        int testPhase = TESTPHASE_NOT_STARTED;
        int downloadCount = 0;
        int uploadCount = 0;
    }

    @Override
    public void runTest(final ServerEntry selectedServer, final NetworkTestingOptions options, OnSpeedtestListener onSpeedtestListener) {
        try {
            IO.Options opts = new IO.Options();
            opts.reconnection = false;
            final Socket socket = IO.socket(selectedServer.getUrl(), opts);
            final OnSpeedtestListener listener = new OnSpeedtestListenerResultToIOProgressWrapper(onSpeedtestListener)
                    .ioProgressOnlyShowsAttemptCount();

            // Variables shared throughout this socket's event listeners.
            final Vars vars = new Vars();

            NetworkTestingOptions actualOptions;
            actualOptions = new NetworkTestingOptions(options);
            actualOptions.setPingCount(Math.max(options.getPingCount(), 2));
            actualOptions.setDownloadCount(Math.max(options.getDownloadCount(), 1));
            actualOptions.setUploadCount(Math.max(options.getUploadCount(), 1));
            actualOptions.setDownloadSize(Math.max(options.getDownloadSize(), 1));
            actualOptions.setUploadSize(Math.max(options.getUploadSize(), 1));
            vars.actualOptions = actualOptions;

            socket.on(Socket.EVENT_CONNECT, new Listener() {

                @Override
                public void call(Object... args) {
                    try {
                        if (vars.testPhase == TESTPHASE_NOT_STARTED) {
                            JSONObject settings = new JSONObject();
                            // server and client fails after
                            settings.put("pingTestCount", vars.actualOptions.getPingCount());
                            // Works if 0 but is treated as 1.
                            // Used Math.max anyway in case 0 raises undefined behavior later.
                            settings.put("dlTestCount", vars.actualOptions.getDownloadCount());
                            settings.put("ulTestCount", vars.actualOptions.getUploadCount());
                            settings.put("challengeCount", 1); // unused
                            settings.put("dlSize", vars.actualOptions.getDownloadSize());
                            settings.put("ulSize", vars.actualOptions.getUploadSize());

                            // Fire the listeners.
                            listener.onTestStart(selectedServer.getHost(), -1, vars.actualOptions);
                            // Ping starts immediately after.
                            listener.onPingStart(vars.actualOptions.getPingCount());

                            vars.testPhase = TESTPHASE_PING;

                            // Emit the things.
                            JSONObject params = new JSONObject();
                            params.put("settings", settings);
                            socket.emit("start", params);
                        }
                    } catch (JSONException e) {
                        // Fail fast.
                        throw new RuntimeException(e);
                    }
                }
            }).on("my_response", new Listener() {
                @Override
                public void call(Object... params) {
                    JSONObject msg = null;
                    // Find the msg object, which should be the only JSONObject.
                    for (int i = 0; i < params.length; i++) {
                        Object o = params[i];
                        if (o instanceof JSONObject) {
                            msg = (JSONObject) o;
                        }
                    }

                    // If given, and is always given as the last item,
                    //      call the callback.
                    if (params[params.length - 1] instanceof Ack) {
                        ((Ack) params[params.length - 1]).call(socket.id());
                    }

                    if (msg != null) {
                        try {
                            onState(msg.getInt("state"), msg, socket);
                        } catch (JSONException e) {
                            // Fail fast.
                            throw new RuntimeException(e);
                        }
                    } else {
                        System.out.println("None of the following params is a JSONObject:");
                        for (int i = 0; i < params.length; i++) {
                            System.out.println(i + ": " + params[i]);
                        }
                    }
                }

                private static final int STATE_DOWNLOAD = 4;
                private static final int STATE_UPLOAD_LOADBIN = 5;
                private static final int STATE_UPLOAD = 6;
                private static final int STATE_RESULTS = 100;
                private static final int STATE_TEST_END = 101;
                private static final int STATE_ERROR = -99;
                private static final String EMIT_NEXT = "next";

                private byte[] ulBinBlob;

                private void onRunningResult(double speed, int currentCount, int totalCount, long size) {
                    if (currentCount<totalCount) {
                        double time = size/speed;
                        long timeN = (long)(size/speed*1000000000);
                        StatisticsReportDouble stats = new StatisticsReportDouble();
                        for (int i=0;i<currentCount;i++) {
                            stats.addValue(speed);
                        }

                        TransferMeasure m = new TransferMeasure(0, timeN, size);

                        if (vars.testPhase == TESTPHASE_DOWNLOAD) {
                            listener.onDownloadResult(m, size, (float) time, stats);
                        } else if (vars.testPhase == TESTPHASE_UPLOAD) {
                            listener.onUploadResult(m, size, (float) time, stats);
                        }
                    } else {
                        // this is last result: this is end of testing.
                        double time = size/speed;
                        long timeN = (long)(size/speed*1000000000);

                        TransferMeasure m = new TransferMeasure(0, timeN, size);
                        List<TransferMeasure> mList = new ArrayList<TransferMeasure>();

                        StatisticsReportDouble stats = new StatisticsReportDouble();
                        for (int i=0;i<currentCount;i++) {
                            stats.addValue(speed);
                            mList.add(m);
                        }

                        if (vars.testPhase == TESTPHASE_DOWNLOAD) {
                            // conclude
                            listener.onDownloadResult(m, size, (float) time, stats);
                            listener.onDownloadEnd(mList, 0, stats);

                            listener.onUploadStart(vars.actualOptions.getUploadCount());

                            // Move forward
                            vars.testPhase = TESTPHASE_UPLOAD;
                        } else if (vars.testPhase == TESTPHASE_UPLOAD) {
                            listener.onUploadResult(m, size, (float) time, stats);
                            listener.onUploadEnd(mList, 0, stats);

                            // Move forward
                            vars.testPhase = TESTPHASE_END;
                        }
                    }
                }

                public void onState(int state, JSONObject msg, Socket s) throws JSONException {
                    JSONObject data = new JSONObject();
                    data.put("state", state);

                    switch (state) {
                        case STATE_DOWNLOAD:
                            byte[] dlBinBlob = (byte[]) msg.get("bin");
                            byte[] last32bytes = new byte[32];
                            System.arraycopy(dlBinBlob, dlBinBlob.length - 32, last32bytes, 0, 32);

                            data.put("hash", last32bytes);

                            s.emit(EMIT_NEXT, data);
                            break;
                        case STATE_UPLOAD_LOADBIN:
                            ulBinBlob = (byte[]) msg.get("bin");

                            s.emit(EMIT_NEXT, data);
                            break;
                        case STATE_UPLOAD:
                            data.put("bin", ulBinBlob);

                            s.emit(EMIT_NEXT, data);
                            break;
                        case STATE_RESULTS:
                            // Called after each PING, DOWNLOAD, or UPLOAD.
                            JSONObject results = msg.getJSONObject("results");

                            double downloadSpeed = results.getDouble("dl")/8;
                            double uploadSpeed = results.getDouble("ul")/8;
                            double pingTime = results.getDouble("rttAve");

                            if (vars.testPhase == TESTPHASE_PING) {
                                // Ping results is only shown once after all the pings have completed.
                                // We'll have to do the appropriate number of callbacks in one go.
                                StatisticsReportDouble stats = new StatisticsReportDouble();
                                List<TransferMeasure> mList = new ArrayList<TransferMeasure>();

                                for (int i=0, c=vars.actualOptions.getPingCount(); i<c; i++) {
                                    TransferMeasure m = new TransferMeasure(0, (long)(pingTime*1000000000), 32);
                                    stats.addValue(pingTime*1000);
                                    mList.add(m);

                                    listener.onPingResult(m, (long)(pingTime*1000), stats);
                                }
                                listener.onPingEnd(mList, 0, stats);

                                listener.onDownloadStart(vars.actualOptions.getDownloadCount());

                                // Move forward.
                                vars.testPhase = TESTPHASE_DOWNLOAD;
                            } else if (vars.testPhase == TESTPHASE_DOWNLOAD) {
                                // The values given by the results are already averaged.
                                vars.downloadCount++;

                                onRunningResult(downloadSpeed, vars.downloadCount, vars.actualOptions.getDownloadCount(), vars.actualOptions.getDownloadSize());
                            } else if (vars.testPhase == TESTPHASE_UPLOAD) {
                                vars.uploadCount++;

                                onRunningResult(uploadSpeed, vars.uploadCount, vars.actualOptions.getUploadCount(), vars.actualOptions.getUploadSize());
                            }

                            break;
                        case STATE_TEST_END:
                            listener.onTestEnd();

                            socket.disconnect();
                            break;
                        case STATE_ERROR:
                            listener.onTestInterrupted(null);

                            socket.disconnect();
                            break;
                        default:
                            s.emit(EMIT_NEXT, data);
                            break;
                    }
                }
            }).on(Socket.EVENT_DISCONNECT, new Listener() {
                @Override
                public void call(Object... params) {
                    if (vars.testPhase != TESTPHASE_END) {
                        listener.onTestInterrupted(new IOException("Disconnected.") {
                            @Override
                            public synchronized Throwable fillInStackTrace() {
                                return this;
                            }
                        });
                    }
                }
            });

            socket.connect();
        } catch (RuntimeException e) {
            // Fail fast.
            throw e;
        } catch (Exception e) {
            // Fail fast.
            throw new RuntimeException(e);
        }
    }
}
