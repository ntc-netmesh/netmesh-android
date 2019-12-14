package net.pregi.math;

/** <p>This allows calculating the statistical properties of an arbitrary number of values
 * as they come. The list of values are not kept and so some properties are unavailable.</p>
 */
public class StatisticsReportDouble {
    private int sampleCount = 0;
    public int getSampleCount() {
        return sampleCount;
    }

    private double mean = 0;
    public double getMean() {
        return mean;
    }

    private double min = Double.MAX_VALUE;
    public double getMin() {
        return min;
    }

    private double max = Double.MIN_VALUE;
    public double getMax() {
        return max;
    }

    /** <p>The current mean difference sum,
     * used in Welford's online algorithm for computing variance.</p>
     */
    private double currentMeanDiffSum;

    private double uncorrectedStandardDeviation = 0;
    public double getUncorrectedStandardDeviation() {
        return uncorrectedStandardDeviation;
    }

    private double correctedStandardDeviation = 0;
    public double getCorrectedStandardDeviation() {
        return correctedStandardDeviation;
    }

    /**<p>Add a sample and update statistical properties.</p>
     *
     * @param value
     */
    public void addValue(double value) {
        sampleCount++;

        // save old mean. we'll need it later.
        double oldMean = mean;

        // mean = (mean*(sampleCount-1)+value)/sampleCount;
        // I'm not sure if "catastrophic cancellation" applies to the above way of computing
        //      the updated mean; the same phenomenon is why I'm using Welford's algorithm.
        // To be on the safe side, I'll use the below way of updating the mean,
        //      derived in a similar fashion as Welford's.
        mean += (value-mean)/sampleCount;

        if (min > value) {
            min = value;
        }

        if (max < value) {
            max = value;
        }

        // Follow Welford's online algorithm so we can compute the variances etc.
        // Said algorithm is actually this simple.
        currentMeanDiffSum += (value-oldMean)*(value-mean);
        uncorrectedStandardDeviation = Math.sqrt(currentMeanDiffSum/sampleCount);
        if (sampleCount>1) {
            correctedStandardDeviation = Math.sqrt(currentMeanDiffSum / (sampleCount - 1));
        }
        uncorrectedStandardDeviation = Math.sqrt(currentMeanDiffSum / sampleCount);
    }

    public StatisticsReportDouble() {

    }
}
