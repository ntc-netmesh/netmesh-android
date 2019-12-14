package net.pregi.networking.speedtest.provider.ookla;

import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.OnSpeedtestListener;
import net.pregi.networking.speedtest.ServerEntry;

public class SpeedtestProcessWithOokla {
    public enum State {
        TEST_STARTING;
    }




    /**<p>Run the test. Pings, downloads, and uploads, in that order, happens sequentially
     * in the same thread that calls this; you must put this in a separate thread to avoid stalling
     * the whole program.</p>
     *
     * @param selectedServer
     * @param options
     */
    public static void runTest(final ServerEntry selectedServer, NetworkTestingOptions options, OnSpeedtestListener onSpeedtestListener) {

    }
}
