package net.pregi.android.netmesh.speedtest.process;

import android.graphics.Color;
import android.text.SpannableStringBuilder;

import net.pregi.android.text.SpanUtils;
import net.pregi.math.StatisticsReportDouble;
import net.pregi.math.StatisticsReportDoubleWithMedian;
import net.pregi.networking.speedtest.IOProcessMode;
import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.OnSpeedtestListener;
import net.pregi.networking.speedtest.TransferMeasure;

import java.util.List;

/** <p>This is a class used to receive speedtest results and log them.</p>
 *
 * <p>Callbacks modify internal variables, and so are not thread-safe.</p>
 */
public abstract class SpeedtestSpannedLogger implements OnSpeedtestListener {
    /** <p>This is where the current log is being built.</p> */
    private SpannableStringBuilder output;
    public CharSequence getOutput() {
        return output;
    }

    private String printByteCount(long byteCount) {
        if (byteCount>=1000) {
            if (byteCount>=1000000) {
                return String.format("%.2f MB", byteCount/1000000f);
            } else {
                return String.format("%.2f KB", byteCount/1000f);
            }
        } else {
            return byteCount+" B";
        }
    }
    private String printSpeed(double bytesPerSecond) {
        if (bytesPerSecond>=1000) {
            if (bytesPerSecond>=1000000) {
                return String.format("%.2f Mbps", bytesPerSecond*8/1000000f);
            } else {
                return String.format("%.2f Kbps", bytesPerSecond*8/1000f);
            }
        } else {
            return bytesPerSecond*8+" bps";
        }
    }
    private void appendThroughputTest(SpannableStringBuilder output, TransferMeasure result) {
        output.append(SpanUtils.bold("Payload: "));
        if (result != null) {
            output.append(printByteCount(result.getByteCount()));
            output.append(String.format(" over %.2f s. ", result.getMilliseconds() / 1000f));
            output.append(SpanUtils.bold("Speed: "));
            output.append(printSpeed(result.getByteCount() * 1000f / result.getMilliseconds()));
            output.append("\n");
        } else {
            output.append(SpanUtils.colored("Failed: there was no payload to measure.\n", Color.RED));
        }
    }
    private void appendThroughputSummary(SpannableStringBuilder output, List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
        output.append(SpanUtils.bold("--- Summary ---\n"));
        if (results.size()>0) {
            double[] sampleBytesPerSecond = new double[results.size()];

            long totalPayload = 0;
            long totalTime = 0;
            int i=0;
            for (TransferMeasure m : results) {
                totalPayload += m.getByteCount();
                totalTime += m.getNanoseconds();
                i++;
            }

            appendThroughputStats(output, totalPayload, totalTime, stats);
            if (failCount>0) {
                output.append(SpanUtils.colored(String.format("Failed %d (%.1f)%% times\n", failCount, (failCount*100f)/(failCount+results.size())), Color.RED));
            }
        } else if (failCount>0) {
            output.append(SpanUtils.colored("All "+failCount+" attempts failed.\n", Color.RED));
        }
    }
    private void appendThroughputStats(SpannableStringBuilder output, long totalPayload, long totalTime, StatisticsReportDouble stats) {
        output.append(SpanUtils.bold("Total successful payload: "));
        output.append(printByteCount(totalPayload));
        output.append("\n");
        output.append(SpanUtils.bold("Total successful time: "));
        output.append(String.format("%.2f s\n", totalTime/1000000000f));
        if (stats instanceof StatisticsReportDoubleWithMedian) {
            StatisticsReportDoubleWithMedian statsWithMedian = (StatisticsReportDoubleWithMedian)stats;
            output.append(SpanUtils.bold("Min / Median / Max: "));
            output.append(String.format("%s / %s / %s\n", printSpeed(statsWithMedian.getMin()), printSpeed(statsWithMedian.getMedian()), printSpeed(statsWithMedian.getMax())));
        } else {
            output.append(SpanUtils.bold("Min / Max: "));
            output.append(String.format("%s / %s\n", printSpeed(stats.getMin()), printSpeed(stats.getMax())));
        }
        output.append(SpanUtils.bold("Average: "));
        output.append(printSpeed(stats.getMean()));
        if (stats.getSampleCount()>1) {
            output.append("\n");
            output.append(SpanUtils.bold("Sample Std. Dev.: "));
            output.append(printSpeed(stats.getCorrectedStandardDeviation()));
        }
        output.append("\n");
    }
    private CharSequence logException(Exception e) {
        return SpanUtils.colored("Failed: "+e.getClass().getSimpleName()+": "+e.getMessage(), Color.RED);
    }

    public abstract void onUpdateLog(CharSequence spannedContent);

    /** <p>Executed when starting the whole test.</p>
     *
     * <p>If you override this to set a flag elsewhere indicating this process's status,
     * call super().</p>
     *
     */
    @Override
    public void onTestStart(String host, int port, NetworkTestingOptions options) {
        output = new SpannableStringBuilder();

        output.append("Conducting tests with ");
        output.append(SpanUtils.italic(host + (port>=0 ? ":" + port : "")));
        output.append("\n");

        onUpdateLog(output);
    }

    @Override
    public void onPingStart(int attempts) {
        output.append(SpanUtils.bold("\n=== PING ===\n"));

        onUpdateLog(output);
    }

    @Override
    public void onPingResult(TransferMeasure result, long milliseconds, StatisticsReportDouble stats) {
        output.append(SpanUtils.bold("Response time: "));
        output.append(String.format("%5d ms", result.getMilliseconds()));
        output.append("\n");

        onUpdateLog(output);
    }

    @Override
    public void onPingException(Exception e) {
        output.append(logException(e));
        output.append("\n");

        onUpdateLog(output);
    }

    @Override
    public void onPingEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
        output.append(SpanUtils.bold("--- Summary ---\n"));

        if (stats instanceof StatisticsReportDoubleWithMedian) {
            StatisticsReportDoubleWithMedian statsWithMedian = (StatisticsReportDoubleWithMedian)stats;
            output.append(SpanUtils.bold("Min / Median / Max: "));
            output.append(String.format("%.2f ms / %.2f ms / %.2f ms\n", statsWithMedian.getMin(), statsWithMedian.getMedian(), statsWithMedian.getMax()));
        } else {
            output.append(SpanUtils.bold("Min / Max: "));
            output.append(String.format("%.2f ms / %.2f ms\n", stats.getMin(), stats.getMax()));
        }
        output.append(SpanUtils.bold("Average: "));
        output.append(String.format("%.2f ms", stats.getMean()));

        if (results.size()>0) {
            if (stats.getSampleCount() > 1) {
                output.append("\n");
                output.append(SpanUtils.bold("Sample Std. Dev.: "));
                output.append(String.format("%.2f ms", stats.getCorrectedStandardDeviation()));
            }
            output.append("\n");
            if (failCount > 0) {
                output.append(SpanUtils.colored(String.format("Failed %d (%.1f)%% times\n", failCount, (failCount * 100f) / (failCount + results.size())), Color.RED));
            }
        } else if (failCount>0) {
            output.append(SpanUtils.colored("All " + failCount + " attempts failed.\n", Color.RED));
        }

        onUpdateLog(output);
    }

    @Override
    public void onIOProgress(long current, long total, IOProcessMode ioProcessMode) {
        // do nothing yet.
    }

    @Override
    public void onDownloadStart(int attempts) {
        output.append(SpanUtils.bold("\n=== DOWNLOAD ===\n"));

        onUpdateLog(output);
    }

    @Override
    public void onDownloadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble stats) {
        appendThroughputTest(output, result);

        onUpdateLog(output);
    }

    @Override
    public void onDownloadException(Exception e) {
        output.append(logException(e));
        output.append("\n");

        onUpdateLog(output);
    }

    @Override
    public void onDownloadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
        appendThroughputSummary(output, results, failCount, stats);

        onUpdateLog(output);
    }

    @Override
    public void onUploadStart(int attempts) {
        output.append(SpanUtils.bold("\n=== UPLOAD ===\n"));

        onUpdateLog(output);
    }

    @Override
    public void onUploadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble stats) {
        appendThroughputTest(output, result);

        onUpdateLog(output);
    }

    @Override
    public void onUploadException(Exception e) {
        output.append(logException(e));
        output.append("\n");

        onUpdateLog(output);
    }

    @Override
    public void onUploadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
        appendThroughputSummary(output, results, failCount, stats);

        onUpdateLog(output);
    }

    @Override
    public void onTestInterrupted(Exception e) {
        output.append(SpanUtils.bold("\nInterrupted" + (e != null ? ": "+e.getClass().getSimpleName()+": "+e.getMessage() : ".")));

        onUpdateLog(output);
    }

    @Override
    public boolean isRequestingTestStop() {
        return false;
    }

    @Override
    public void onTestEnd() {
        output.append(SpanUtils.bold("\nFinished."));

        onUpdateLog(output);
    }
}
