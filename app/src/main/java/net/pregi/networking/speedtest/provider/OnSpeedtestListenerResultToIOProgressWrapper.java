package net.pregi.networking.speedtest.provider;

import net.pregi.math.StatisticsReportDouble;
import net.pregi.networking.speedtest.IOProcessMode;
import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.OnSpeedtestListener;
import net.pregi.networking.speedtest.TransferMeasure;

import java.util.List;

/** <p>The download/upload process reports ioProgress as (current/total) of only the current
 * attempt. This class turns it to (currentOverall/totalOverall), taking into account
 * previous attempts, so any gauge displaying the process progress displays the whole set
 * of attempts.</p>
 *
 */
public class OnSpeedtestListenerResultToIOProgressWrapper implements OnSpeedtestListener {
    private OnSpeedtestListener base;

    private int attempts;
    private int completedAttempts;
    private long payloadPerAttempt;

    private long downloadPayload;
    private long uploadPayload;

    @Override
    public void onTestStart(String host, int port, NetworkTestingOptions options) {
        downloadPayload = options.getDownloadSize();
        uploadPayload = options.getUploadSize();

        base.onTestStart(host, port, options);
    }

    @Override
    public void onPingStart(int attempts) {
        this.attempts = attempts;
        completedAttempts = 0;
        payloadPerAttempt = 1;

        base.onPingStart(attempts);

        // ioProgressOnlyShowsAttemptCount is always true for pings.
        completedAttempts++;
        base.onIOProgress(completedAttempts, attempts, IOProcessMode.PING);
    }

    @Override
    public void onPingResult(TransferMeasure result, long milliseconds, StatisticsReportDouble currentStats) {
        completedAttempts++;
        base.onIOProgress(completedAttempts, attempts, IOProcessMode.PING);

        base.onPingResult(result, milliseconds, currentStats);
    }

    @Override
    public void onPingException(Exception e) {
        completedAttempts++;
        base.onIOProgress(completedAttempts, attempts, IOProcessMode.PING);

        base.onPingException(e);
    }

    @Override
    public void onPingEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
        base.onPingEnd(results, failCount, stats);
    }

    @Override
    public void onDownloadStart(int attempts) {
        this.attempts = attempts;
        completedAttempts = 0;
        payloadPerAttempt = downloadPayload;

        base.onDownloadStart(attempts);

        if (ioProgressOnlyShowsAttemptCount) {
            completedAttempts++;
            base.onIOProgress(completedAttempts, attempts, IOProcessMode.DOWNLOAD);
        }
    }

    @Override
    public void onDownloadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats) {
        completedAttempts++;
        base.onIOProgress(completedAttempts*payloadPerAttempt, attempts*payloadPerAttempt, IOProcessMode.DOWNLOAD);

        base.onDownloadResult(result, byteCount, seconds, currentStats);
    }

    @Override
    public void onDownloadException(Exception e) {
        completedAttempts++;
        base.onIOProgress(completedAttempts*payloadPerAttempt, attempts*payloadPerAttempt, IOProcessMode.DOWNLOAD);

        base.onDownloadException(e);
    }

    @Override
    public void onDownloadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
        base.onDownloadEnd(results, failCount, stats);
    }

    @Override
    public void onUploadStart(int attempts) {
        this.attempts = attempts;
        completedAttempts = 0;
        payloadPerAttempt = uploadPayload;

        base.onUploadStart(attempts);

        if (ioProgressOnlyShowsAttemptCount) {
            completedAttempts++;
            base.onIOProgress(completedAttempts, attempts, IOProcessMode.UPLOAD);
        }
    }

    @Override
    public void onUploadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats) {
        completedAttempts++;
        base.onIOProgress(completedAttempts*payloadPerAttempt, attempts*payloadPerAttempt, IOProcessMode.UPLOAD);

        base.onUploadResult(result, byteCount, seconds, currentStats);
    }

    @Override
    public void onUploadException(Exception e) {
        completedAttempts++;
        base.onIOProgress(completedAttempts*payloadPerAttempt, attempts*payloadPerAttempt, IOProcessMode.UPLOAD);

        base.onUploadException(e);
    }

    @Override
    public void onUploadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
        base.onUploadEnd(results, failCount, stats);
    }

    @Override
    public void onTestInterrupted(Exception e) {
        base.onTestInterrupted(null);
    }

    @Override
    public void onTestEnd() {
        base.onTestEnd();
    }

    @Override
    public boolean isRequestingTestStop() {
        return base.isRequestingTestStop();
    }

    @Override
    public void onIOProgress(long current, long total, IOProcessMode ioProcessMode) {
        base.onIOProgress((completedAttempts+(ioProgressOnlyShowsAttemptCount ? -1 : 0))*payloadPerAttempt+current,
                attempts*payloadPerAttempt,
                ioProcessMode);
    }

    public OnSpeedtestListenerResultToIOProgressWrapper(OnSpeedtestListener base) {
        this.base = base;
    }

    private boolean ioProgressOnlyShowsAttemptCount = false;
    public OnSpeedtestListenerResultToIOProgressWrapper ioProgressOnlyShowsAttemptCount() {
        ioProgressOnlyShowsAttemptCount = true;

        return this;
    }
}
