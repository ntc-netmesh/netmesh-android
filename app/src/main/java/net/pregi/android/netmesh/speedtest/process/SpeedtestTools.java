package net.pregi.android.netmesh.speedtest.process;

import android.content.Context;
import android.util.Log;

import net.pregi.lang.OneThread;
import net.pregi.math.StatisticsReportDouble;
import net.pregi.networking.speedtest.IOProcessMode;
import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.ServerEntry;
import net.pregi.networking.speedtest.provider.SpeedtestProvider;
import net.pregi.networking.speedtest.provider.asti.SpeedtestASTISagoGulamanProvider;
import net.pregi.networking.speedtest.TransferMeasure;
import net.pregi.networking.speedtest.provider.ookla.SpeedtestOoklaProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

/** <p>A rewrite of SpeedtestViewModel, implemented as a singleton for use anywhere in the app,
 * with notifications of state and value changes delivered via listeners,
 * allowing multiple activities or instances of them to display the same running process.</p>
 *
 * <p>Ideally, there should be safeguards from unknowingly duplicating process runs;
 * any activity accessing this class should be able to display what its state currently is,
 * even if the ongoing process was started by a different activity instance.</p>
 */
public class SpeedtestTools {
    public enum Provider {
        OOKLA, ASTI;
    }

    private ServerEntry selectedServer;
    public ServerEntry setSelectedServer(ServerEntry value) {
        // It is not as simple as just setting the value.
        // This should be disabled if a test is running.
        synchronized (listeners) {
            if (selectedServer != value) {
                // The value object passed here should come from the server list.
                // This means plain equality should work.
                for (SpeedtestToolsListener l : listeners) {
                    l.onChangeSelectedServer(value);
                }
            }
            selectedServer = value;
        }

        // return the server actually set.
        // It may be different if another process is preventing this.
        return selectedServer;
    }
    public ServerEntry getSelectedServer() {
        return selectedServer;
    }

    private static boolean isDead(Thread t) {
        // t should become null at the end of its run().
        // On the paranoid chance that the thread is unceremoniously killed before it can do so,
        //      checking isAlive() will clue us in.
        return t == null || !t.isAlive();
    }

    private OneThread oneThread = new OneThread();
    private List<ServerEntry> serverList;

    /**<p>Returns the last result of downloadServerList().</p>
     *
     * @return null if it was never called or the last call resulted in an exception.
     */
    public List<ServerEntry> getServerList() {
        return serverList;
    }
    /** <p>Downloads the server list. Listeners' onDownloadServerList(list, null) and onChangeSelectedServer(null)
     * will be called, in that order.</p>
     * <p>This call has no effect if another process is already running.</p>
     *
     * @return whether this call started the process of downloading a server list. A result of false
     *          may be used in a user interface to indicate that another process that should not be
     *          run concurrently with this one may still be running.
     */
    public boolean downloadServerList() {
        return oneThread.start(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("downloadServerList", "Downloading server list...");
                    List<ServerEntry> receivedList = speedtestProvider.downloadServerList();

                    Log.i("downloadServerList", "Downloaded server list " + receivedList.size() + " item(s) long.");
                    synchronized (listeners) {
                        serverList = receivedList;
                        selectedServer = receivedList.size() >= 1 ? receivedList.get(0) : null;

                        for (SpeedtestToolsListener l : listeners) {
                            l.onDownloadServerList(receivedList, null);
                            l.onChangeSelectedServer(selectedServer);
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof IOException) {
                        // May or may not be necessary...
                        e.printStackTrace();
                    } else {
                        // Totally necessary if we're getting unexpected exceptions.
                        e.printStackTrace();
                    }

                    synchronized (listeners) {
                        serverList = null;
                        selectedServer = null;

                        for (SpeedtestToolsListener l : listeners) {
                            l.onDownloadServerList(null, e);
                            l.onChangeSelectedServer(selectedServer);
                        }
                    }
                }
            }
        });
    }

    private NetworkTestingOptions speedtestOptions;

    /** <p>Set the options. This call is ineffective if a test is running.</p>
     *
     * @param value
     * @return whether the options have been set.
     */
    public boolean setSpeedtestOptions(NetworkTestingOptions value) {
        speedtestOptions = value;
        return true;
    }
    public NetworkTestingOptions getSpeedtestOptions() {
        return speedtestOptions;
    }

    private enum SpeedtestState {
        STARTED,
        PING_STARTED,
        PING_ENDED,
        DOWNLOAD_STARTED,
        DOWNLOAD_ENDED,
        UPLOAD_STARTED,
        UPLOAD_ENDED,
        ENDED,
        INTERRUPTED
    }
    private SpeedtestState speedtestState = null;
    /** <p>A copy of the speedtest options is set here when a test is running,
     * ensuring that an unadulterated copy of them is available to this class.
     * </p>
     *
     */
    private NetworkTestingOptions activeSpeedtestOptions;
    private String activeSpeedtestHost;
    private int activeSpeedtestPort;

    private class PingResult {
        TransferMeasure result;
        long milliseconds;

        /** Almost useless since all PingResult instances will share the same object. */
        StatisticsReportDouble currentStats;

        Exception e;

        PingResult(TransferMeasure result, long milliseconds, StatisticsReportDouble currentStats) {
            this.result = result;
            this.milliseconds = milliseconds;
            this.currentStats = currentStats;
        }
        PingResult(Exception e) {
            this.e = new SummarizedException(e);
        }
    }
    private List<PingResult> pingResults = new ArrayList<PingResult>();

    private class ThroughputResult {
        TransferMeasure result;
        long byteCount;
        float seconds;

        /** Almost useless since all PingResult instances will share the same object. */
        StatisticsReportDouble currentStats;

        Exception e;

        ThroughputResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats) {
            this.result = result;
            this.byteCount = byteCount;
            this.seconds = seconds;
            this.currentStats = currentStats;
        }
        ThroughputResult(Exception e) {
            this.e = new SummarizedException(e);
        }
    }
    private List<ThroughputResult> downloadResults = new ArrayList<ThroughputResult>();
    private List<ThroughputResult> uploadResults = new ArrayList<ThroughputResult>();
    private StatisticsReportDouble lastPingStats, lastDownloadStats, lastUploadStats;

    private SpeedtestSpannedLogger speedtestEventDispatcher = new SpeedtestSpannedLogger() {
        @Override
        public void onTestStart(String host, int port, NetworkTestingOptions options) {
            super.onTestStart(host, port, options);
            activeSpeedtestHost = host;
            activeSpeedtestPort = port;
            synchronized (listeners) {
                pingResults.clear();
                downloadResults.clear();
                uploadResults.clear();
                speedtestState = SpeedtestState.STARTED;
                activeSpeedtestOptions = options;
                for (SpeedtestToolsListener l : listeners) {
                    l.onTestStart(host, port, options);
                }
            }
        }

        @Override
        public void onPingStart(int attempts) {
            super.onPingStart(attempts);
            synchronized (listeners) {
                pingResults.clear();
                lastPingStats = null;
                speedtestState = SpeedtestState.PING_STARTED;
                for (SpeedtestToolsListener l : listeners) {
                    l.onPingStart(attempts);
                }
            }
        }

        @Override
        public void onPingResult(TransferMeasure result, long milliseconds, StatisticsReportDouble currentStats) {
            super.onPingResult(result, milliseconds, currentStats);
            synchronized (listeners) {
                if (result != null) {
                    pingResults.add(new PingResult(result, milliseconds, currentStats));
                }
                for (SpeedtestToolsListener l : listeners) {
                    l.onPingResult(result, milliseconds, currentStats);
                }
            }
        }

        @Override
        public void onPingException(Exception e) {
            super.onPingException(e);
            synchronized (listeners) {
                pingResults.add(new PingResult(e));
                for (SpeedtestToolsListener l : listeners) {
                    l.onPingException(e);
                }
            }
        }

        @Override
        public void onPingEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
            super.onPingEnd(results, failCount, stats);
            synchronized (listeners) {
                speedtestState = SpeedtestState.PING_ENDED;
                lastPingStats = stats;
                for (SpeedtestToolsListener l : listeners) {
                    l.onPingEnd(results, failCount, stats);
                }
            }
        }

        @Override
        public void onIOProgress(long current, long total, IOProcessMode ioProcessMode) {
            // TODO: decide whether this is good, as it is possible to contrive code
            //  such that this step mess up readings, since this takes place between timestamps.
            super.onIOProgress(current, total, ioProcessMode);
            synchronized (listeners) {
                for (SpeedtestToolsListener l : listeners) {
                    l.onIOProgress(current, total, ioProcessMode);
                }
            }
        }

        @Override
        public void onDownloadStart(int attempts) {
            super.onDownloadStart(attempts);
            synchronized (listeners) {
                downloadResults.clear();
                speedtestState = SpeedtestState.DOWNLOAD_STARTED;
                lastDownloadStats = null;
                for (SpeedtestToolsListener l : listeners) {
                    l.onDownloadStart(attempts);
                }
            }
        }

        @Override
        public void onDownloadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats) {
            super.onDownloadResult(result, byteCount, seconds, currentStats);
            synchronized (listeners) {
                if (result != null) {
                    downloadResults.add(new ThroughputResult(result, byteCount, seconds, currentStats));
                }
                for (SpeedtestToolsListener l : listeners) {
                    l.onDownloadResult(result, byteCount, seconds, currentStats);
                }
            }
        }

        @Override
        public void onDownloadException(Exception e) {
            super.onDownloadException(e);
            synchronized (listeners) {
                downloadResults.add(new ThroughputResult(e));
                for (SpeedtestToolsListener l : listeners) {
                    l.onDownloadException(e);
                }
            }
        }

        @Override
        public void onDownloadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
            super.onDownloadEnd(results, failCount, stats);
            synchronized (listeners) {
                speedtestState = SpeedtestState.DOWNLOAD_ENDED;
                lastDownloadStats = stats;
                for (SpeedtestToolsListener l : listeners) {
                    l.onDownloadEnd(results, failCount, stats);
                }
            }
        }

        @Override
        public void onUploadStart(int attempts) {
            super.onUploadStart(attempts);
            synchronized (listeners) {
                uploadResults.clear();
                speedtestState = SpeedtestState.UPLOAD_STARTED;
                lastUploadStats = null;
                for (SpeedtestToolsListener l : listeners) {
                    l.onUploadStart(attempts);
                }
            }
        }

        @Override
        public void onUploadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats) {
            super.onUploadResult(result, byteCount, seconds, currentStats);
            synchronized (listeners) {
                if (result != null) {
                    uploadResults.add(new ThroughputResult(result, byteCount, seconds, currentStats));
                }
                for (SpeedtestToolsListener l : listeners) {
                    l.onUploadResult(result, byteCount, seconds, currentStats);
                }
            }
        }

        @Override
        public void onUploadException(Exception e) {
            super.onUploadException(e);
            synchronized (listeners) {
                uploadResults.add(new ThroughputResult(e));
                for (SpeedtestToolsListener l : listeners) {
                    l.onUploadException(e);
                }
            }
        }

        @Override
        public void onUploadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
            super.onUploadEnd(results, failCount, stats);
            synchronized (listeners) {
                speedtestState = SpeedtestState.UPLOAD_ENDED;
                lastUploadStats = stats;
                for (SpeedtestToolsListener l : listeners) {
                    l.onUploadEnd(results, failCount, stats);
                }
            }
        }

        @Override
        public void onTestInterrupted(Exception e) {
            super.onTestInterrupted(e);
            synchronized (listeners) {
                speedtestState = SpeedtestState.INTERRUPTED;
                for (SpeedtestToolsListener l : listeners) {
                    l.onTestInterrupted(null);
                }
            }
        }

        @Override
        public void onTestEnd() {
            super.onTestEnd();
            synchronized (listeners) {
                speedtestState = SpeedtestState.ENDED;
                for (SpeedtestToolsListener l : listeners) {
                    l.onTestEnd();
                }
            }
        }

        @Override
        public boolean isRequestingTestStop() {
            boolean someone = false;
            synchronized (listeners) {
                for (SpeedtestToolsListener l : listeners) {
                    // Allow everyone's isRequestingTestStop() to be called
                    //  instead of breaking ahead of time.
                    someone |= l.isRequestingTestStop();
                }
            }
            return someone;
        }

        @Override
        public void onUpdateLog(CharSequence spannedContent) {
            synchronized (listeners) {
                for (SpeedtestToolsListener l : listeners) {
                    l.onUpdateLog(spannedContent);
                }
            }
        }
    };

    /** <p>A unified listener of all things SpeedtestTools.</p>
     *
     * <p>Activities should add this on their onStart(), and remove it when done.</p>
     */
    private final List<SpeedtestToolsListener> listeners = new LinkedList<SpeedtestToolsListener>();
    /** <p>Add a listener. Whatever provides this listener must remove it  at the appropriate
     * lifecycle state to avoid memory leaks.</p>
     */
    public SpeedtestToolsListener addListener(SpeedtestToolsListener l) {
        // I'm deciding whether it's gonna be as simple as adding the listener,
        //      or require providing a context to see if this l is still relevant later
        //      to avoid memory leaks.
        // TODO: decide whether to add another interface method called "isAbandoned"
        //      and require that listeners return true if the listener has no more use to them
        //      (e.g. when their activity is removed, or when they're dead)
        synchronized (listeners) {
            listeners.add(l);
            if (serverList != null) {
                l.onDownloadServerList(serverList, null);
                l.onChangeSelectedServer(selectedServer);
            } else {
                l.onDownloadServerList(null, null);
                l.onChangeSelectedServer(null);
            }

            l.onUpdateLog(speedtestEventDispatcher.getOutput());

            // For those late to the party or those coming back from the bathroom,
            // update them on what they missed.
            if (speedtestState != null) {
                l.onTestStart(activeSpeedtestHost, activeSpeedtestPort, activeSpeedtestOptions);
                if (!pingResults.isEmpty() || speedtestState == SpeedtestState.PING_STARTED || speedtestState == SpeedtestState.PING_ENDED) {
                    l.onPingStart(activeSpeedtestOptions.getPingCount());
                }
                if (!pingResults.isEmpty()) {
                    int failCount = 0;
                    List<TransferMeasure> results = new ArrayList<TransferMeasure>();
                    for (PingResult p : pingResults) {
                        if (p.e != null) {
                            failCount++;
                            l.onPingException(p.e);
                        } else {
                            l.onPingResult(p.result, p.milliseconds, p.currentStats);
                            results.add(p.result);
                        }
                    }

                    if (speedtestState !=  SpeedtestState.PING_STARTED) {
                        l.onPingEnd(results, failCount, lastPingStats);
                    }
                }
                if (!downloadResults.isEmpty() || speedtestState == SpeedtestState.DOWNLOAD_STARTED || speedtestState == SpeedtestState.DOWNLOAD_ENDED) {
                    l.onDownloadStart(activeSpeedtestOptions.getDownloadCount());
                }
                if (!downloadResults.isEmpty()) {
                    int failCount = 0;
                    List<TransferMeasure> results = new ArrayList<TransferMeasure>();
                    for (ThroughputResult r : downloadResults) {
                        if (r.e != null) {
                            failCount++;
                            l.onDownloadException(r.e);
                        } else {
                            l.onDownloadResult(r.result, r.byteCount, r.seconds, r.currentStats);
                            results.add(r.result);
                        }
                    }

                    if (speedtestState !=  SpeedtestState.DOWNLOAD_STARTED) {
                        l.onDownloadEnd(results, failCount, lastDownloadStats);
                    }
                }
                if (!uploadResults.isEmpty() || speedtestState == SpeedtestState.UPLOAD_STARTED || speedtestState == SpeedtestState.UPLOAD_ENDED) {
                    l.onUploadStart(activeSpeedtestOptions.getUploadCount());
                }
                if (!uploadResults.isEmpty()) {
                    int failCount = 0;
                    List<TransferMeasure> results = new ArrayList<TransferMeasure>();
                    for (ThroughputResult r : uploadResults) {
                        if (r.e != null) {
                            failCount++;
                            l.onUploadException(r.e);
                        } else {
                            l.onUploadResult(r.result, r.byteCount, r.seconds, r.currentStats);
                            results.add(r.result);
                        }
                    }

                    if (speedtestState !=  SpeedtestState.UPLOAD_STARTED) {
                        l.onUploadEnd(results, failCount, lastUploadStats);
                    }
                }

                if (speedtestState == SpeedtestState.ENDED) {
                    l.onTestEnd();
                } else if (speedtestState == SpeedtestState.INTERRUPTED) {
                    l.onTestInterrupted(null);
                }
            }
        }
        return l;
    }
    /** <p>Remove a listener. Whatever previously added that listener must remove it at the
     * appropriate lifecycle state to avoid memory leaks.</p>
     */
    public void removeListener(SpeedtestToolsListener l) {
        listeners.remove(l);
    }

    private SpeedtestProvider speedtestProvider;
    private Provider speedtestProviderEnum;
    public Provider getProviderEnum() {
        return speedtestProviderEnum;
    }
    public Object getConstraint(SpeedtestProvider.Constraint constraint) {
        return speedtestProvider.getConstraint(constraint);
    }

    /** <p>Start the speedtest using the currently set selected server and options.</p>
     * <p>This call has no effect if another process is already running.</p>
     *
     * @return whether this call started the process of speedtesting. A result of false
     *          may be used in a user interface to indicate that another process that should not be
     *          run concurrently with this one may still be running.
     */
    public boolean runTest() {
        return oneThread.start(new Runnable() {
            @Override
            public void run() {
                speedtestProvider.runTest(selectedServer, speedtestOptions, speedtestEventDispatcher);
            }
        });
    }

    public boolean runExamineNetworks(final Context context) {
        return oneThread.start(new Runnable() {
            @Override
            public void run() {
                ExamineNetworksProcessB.examineNetworks(context, new ExamineNetworksProcessB.OnLogListener() {
                    @Override
                    public void onLog(CharSequence log) {
                        speedtestEventDispatcher.onUpdateLog(log);
                    }
                });
            }
        });
    }

    public boolean runCheckPermissions(final Context context) {
        return oneThread.start(new Runnable() {
            @Override
            public void run() {
                CheckPermissionsProcessB.checkPermissions(context, new CheckPermissionsProcessB.OnLogListener() {
                    @Override
                    public void onLog(CharSequence log) {
                        speedtestEventDispatcher.onUpdateLog(log);
                    }
                });
            }
        });
    }


    public boolean isRunning() {
        return oneThread.isRunning();
    }

    private static EnumMap<Provider, SpeedtestTools>  _instances = new EnumMap<Provider, SpeedtestTools>(Provider.class);
    private SpeedtestTools(Provider provider) {
        speedtestProviderEnum = provider;
        switch (provider) {
        case OOKLA:
            speedtestProvider = new SpeedtestOoklaProvider();
            break;
        case ASTI:
        default:
            speedtestProvider = new SpeedtestASTISagoGulamanProvider();
            speedtestProviderEnum = Provider.ASTI;
            break;
        }
    }
    public static synchronized SpeedtestTools getInstance(Provider provider) {
        SpeedtestTools out = _instances.get(provider);

        if (out == null) {
            out = new SpeedtestTools(provider);
            out.downloadServerList();

            _instances.put(provider, out);
        }
        return out;
    }
}
