package net.pregi.networking.speedtest.provider;

import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.OnSpeedtestListener;
import net.pregi.networking.speedtest.ServerEntry;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public abstract class SpeedtestProvider {
    public enum Constraint {
        HTTPS_TOGGLEABLE,
        PING_COUNT_MIN,
        DOWNLOAD_COUNT_MIN,
        DOWNLOAD_SIZE_MIN,
        UPLOAD_COUNT_MIN,
        UPLOAD_SIZE_MIN;
    }
    private Map<Constraint, Object> parameter = new EnumMap<Constraint, Object>(Constraint.class);
    public Object getConstraint(Constraint key) {
        return parameter.get(key);
    }

    /** <p>Set a value to a parameter. Use this to define certain aspects of the provider
     * that may be specific to it.</p>
     *
     * @param key
     * @param value
     */
    protected void setConstraint(Constraint key, Object value) {
        parameter.put(key, value);
    }


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