package net.pregi.networking.speedtest;

import net.pregi.math.StatisticsReportDouble;

import java.util.List;

public interface OnSpeedtestListener extends OnIOProgressListener {
    public void onTestStart(String host, int port, NetworkTestingOptions options);

    public void onPingStart(int attempts);
    public void onPingResult(TransferMeasure result, long milliseconds, StatisticsReportDouble currentStats);
    public void onPingException(Exception e);
    public void onPingEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats);

    public void onDownloadStart(int attempts);
    public void onDownloadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats);
    public void onDownloadException(Exception e);
    public void onDownloadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats);

    public void onUploadStart(int attempts);
    public void onUploadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats);
    public void onUploadException(Exception e);
    public void onUploadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats);

    /** <p>Called when the speedtest process did not run its full course.</p>
     *
     * <p>onTestEnd() is NOT called afterwards, but if you have nothing special onTestInterrupted(),
     * and need to do something onTestEnd(), or your onTestInterrupted() needs to do something
     * similar to onTestEnd(), you may simply call that function yourself when overriding.</p>
     * @param e
     */
    public void onTestInterrupted(Exception e);
    /** <p>Called when the speedtest process completed.
     * It is NOT called when onTestInterrupted() happened.</p>
     */
    public void onTestEnd();

    /** <p>Indicates that the listener wants the test to stop.</p>
     *
     * @return true to ask the test to stop; false otherwise.
     */
    public boolean isRequestingTestStop();
}
