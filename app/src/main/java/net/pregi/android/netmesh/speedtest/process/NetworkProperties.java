package net.pregi.android.netmesh.speedtest.process;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetworkProperties {
    private String networkOperator;
    public String getNetworkOperator() {
        return networkOperator;
    }

    private String networkOperatorName;
    /** <p>Get the full network operator name.
     * Only given if {@link NetworkProperties#getActiveNetworkType() getActiveNetworkType()}
     * is {@link ConnectivityManager#TYPE_MOBILE ConnectivityManager.TYPE_MOBILE}</p>*/
    public String getNetworkOperatorName() {
        return networkOperatorName;
    }

    private int networkType;
    public int getNetworkType() {
        return networkType;
    }

    private String simOperator;
    public String getSimOperator() {
        return simOperator;
    }

    private String simOperatorName;
    public String getSimOperatorName() {
        return simOperatorName;
    }

    private Integer activeNetworkType;
    /** <p>Get the network type. If given, it will be one of ConnectivityManager.TYPE_*</p> */
    public Integer getActiveNetworkType() {
        return activeNetworkType;
    }

    private String activeNetworkTypeName;
    public String getActiveNetworkTypeName() {
        return activeNetworkTypeName;
    }

    private Integer activeNetworkSubtype;
    /** <p>Get the network subtype. If given, it will be one of TelephonyManager.NETWORK_TYPE_*</p> */
    public Integer getActiveNetworkSubtype() {
        return activeNetworkSubtype;
    }

    private String activeNetworkSubtypeName;
    public String getActiveNetworkSubtypeName() {
        return activeNetworkSubtypeName;
    }

    private String extraInfo;
    public String getExtraInfo() {
        return extraInfo;
    }

    private String reason;
    public String getFailureReason() {
        return reason;
    }

    private boolean isConnected;
    public boolean getIsConnected() {
        return isConnected;
    }

    private NetworkProperties(Context context) {
        TelephonyManager tm = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE));
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        networkOperator = tm.getNetworkOperator();
        networkOperatorName = tm.getNetworkOperatorName();
        networkType = tm.getNetworkType();
        simOperator = tm.getSimOperator();
        simOperatorName = tm.getSimOperatorName();

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null) {
            activeNetworkType = networkInfo.getType(); // mobile or wifi
            activeNetworkTypeName = networkInfo.getTypeName();
            activeNetworkSubtype = networkInfo.getSubtype();
            activeNetworkSubtypeName = networkInfo.getSubtypeName();
            extraInfo = networkInfo.getExtraInfo();
            reason = networkInfo.getReason();
            isConnected = networkInfo.isConnected();

            if (activeNetworkType == ConnectivityManager.TYPE_MOBILE) {
                // only attempt to retrieve mobile carrier info if current network is MOBILE.
                // Beware that some methods (like TelephonyManager#getNetworkOperatorName())
                //      does not take into account multiple SIM cards;
                //      it may return the value for SIM #1 even if the currently connected network
                //      is SIM #2.

                // NOTE: this code assumes that the phone is connected to only one "cell" at a time.

            }
        }
    }

    public static NetworkProperties getProperties(Context context) {
        return new NetworkProperties(context);
    }
}
