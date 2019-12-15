package net.pregi.networking.speedtest;

/** <p>Defines the options for network testing.</p>
 *
 * <p>Initial values are their default values.</p>
 *
 */
public class NetworkTestingOptions {
    private boolean useHttps = false;
    public void setUseHttps(boolean value) {
        useHttps = value;
    }
    public boolean getUseHttps() {
        return useHttps;
    }

    private int pingCount = 10;
    public void setPingCount(int value) {
        pingCount = value;
    }
    public int getPingCount() {
        return pingCount;
    }

    private long downloadSize = 5*1000*1000;
    public void setDownloadSize(long value) {
        downloadSize = value;
    }
    public void setDownloadSize(long value, int thousandsPower) {
        while (thousandsPower>0) {
            value *= 1000;
            thousandsPower--;
        }
        setDownloadSize(value);
    }
    public long getDownloadSize() {
        return downloadSize;
    }

    private int downloadCount = 10;
    public void setDownloadCount(int value) {
        downloadCount = value;
    }
    public int getDownloadCount() {
        return downloadCount;
    }

    private long uploadSize = 2*1000*1000;
    public void setUploadSize(long value) {
        uploadSize = value;
    }
    public void setUploadSize(long value, int thousandsPower) {
        while (thousandsPower>0) {
            value *= 1000;
            thousandsPower--;
        }
        setUploadSize(value);
    }
    public long getUploadSize() {
        return uploadSize;
    }

    private int uploadCount = 10;
    public void setUploadCount(int value) {
        uploadCount = value;
    }
    public int getUploadCount() {
        return uploadCount;
    }

    public NetworkTestingOptions() {

    }
    public NetworkTestingOptions(NetworkTestingOptions copyFrom) {
        if (copyFrom != null) {
            useHttps = copyFrom.useHttps;
            pingCount = copyFrom.pingCount;
            downloadCount = copyFrom.downloadCount;
            downloadSize = copyFrom.downloadSize;
            uploadCount = copyFrom.uploadCount;
            uploadSize = copyFrom.uploadSize;
        }
    }
}
