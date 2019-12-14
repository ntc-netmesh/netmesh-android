package net.pregi.networking.speedtest.provider;

import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.OnSpeedtestListener;
import net.pregi.networking.speedtest.ServerEntry;

import java.io.IOException;
import java.util.List;

public abstract class SpeedtestProvider {
    private String providerName;
    protected void setProviderName(String value) {
        providerName = value;
    }
    public String getProviderName() {
        return providerName;
    }

    public abstract List<ServerEntry> downloadServerList() throws IOException;
    public abstract void runTest(ServerEntry selectedServer, NetworkTestingOptions options, OnSpeedtestListener onSpeedtestListener);
}