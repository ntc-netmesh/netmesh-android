package net.pregi.networking.speedtest;

public interface OnIOProgressListener {
    /**<p>Called when a process is in the process of downloading/uploading data.</p>
     *
     * @param current
     * @param total
     */
    public void onIOProgress(long current, long total, IOProcessMode ioProcessMode);
}
