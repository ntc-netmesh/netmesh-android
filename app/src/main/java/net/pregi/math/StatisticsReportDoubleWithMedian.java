package net.pregi.math;

import java.util.Arrays;

public class StatisticsReportDoubleWithMedian extends StatisticsReportDouble {
    private double median;
    public double getMedian() {
        return median;
    }

    @Override
    public void addValue(double v) {
        // unless we can retain the samples in an efficient self-sorting collection
        //      that allows duplicates (TreeSet is therefore out of the question)
        // indicate that we can't do this at the moment.
        throw new UnsupportedOperationException("Cannot add a new sample to this statistics report.");
    }

    public StatisticsReportDoubleWithMedian(double[] immutableValueArray) {
        super();

        for (double i : immutableValueArray) {
            super.addValue(i);
        }

        double[] copy = Arrays.copyOf(immutableValueArray, immutableValueArray.length);
        Arrays.sort(copy);
        int count = getSampleCount();
        if (count>0) {
            if (count % 2 == 0) {
                median = (copy[count / 2 - 1] + copy[count / 2]) / 2;
            } else {
                median = copy[(count - 1) / 2];
            }
        }
    }
}
