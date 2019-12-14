package net.pregi.android.speedtester.speedtest.ui.gui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

class LinearGauge extends Gauge {
    /** <p></p>This is the list of interval steps.
     * As the displayed value grows larger, the interval steps should go larger as well.</p>
     *
     * <p>The last value in the list is used instead for multiplying intervalStepValueTimes
     * before resetting the index.</p>
     */
    private static final float[] INTERVAL_STEPS_LINEAR
            = new float[] {1, 5, 10, 20, 100, 200, 1000};

    private int intervalStepIndex = 0;
    /** <p>This tracks the current scale of the gauge.
     * For example, if it's meant to display values in the order of thousands,
     *      this value should be 1000.</p>
     * <p>This starts with 1.
     * When a given value goes above the max displayable value, increment index by 1.
     * If the index would go outside the array, set index to 0
     * and set the stepValueTimes to a higher value, like 1000.</p>
     */
    private float intervalStepValueTimes = 1;
    private float intervalStepActualValue = 1;
    private float maximumDisplayableValue;

    private DecimalFormat df10 = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.getDefault()));
    private DecimalFormat df1000 = new DecimalFormat("#", new DecimalFormatSymbols(Locale.getDefault()));

    private double value = 0;
    public void setValue(double value) {
        this.value = value;

        while (value>maximumDisplayableValue) {
            stepUpScale();
        }

        setLevel(value/maximumDisplayableValue);
    }

    @Override
    public void reset() {
        super.reset();

        // notes for linear display.
        // logarithmic don't go by these rules.
        intervalStepIndex = 0;
        intervalStepValueTimes = 1;

        intervalStepActualValue = intervalStepValueTimes*INTERVAL_STEPS_LINEAR[intervalStepIndex];
        maximumDisplayableValue = intervalStepActualValue*getMajorMarksCount();

        while (value>maximumDisplayableValue) {
            stepUpScale();
        }
    }
    private void stepUpScale() {
        intervalStepIndex++;
        if (intervalStepIndex >= INTERVAL_STEPS_LINEAR.length-1) {
            intervalStepIndex = 0;
            intervalStepValueTimes *= INTERVAL_STEPS_LINEAR[INTERVAL_STEPS_LINEAR.length-1];
        }
        intervalStepActualValue = intervalStepValueTimes*INTERVAL_STEPS_LINEAR[intervalStepIndex];
        maximumDisplayableValue = intervalStepActualValue*getMajorMarksCount();
    }

    private static final String[] SUFFIX_INDEX = new String[] {"", "K", "M", "G", "T", "P"};
    private String formatValue(double value, int suffixIndex) {
        if (value<10) {
            return df10.format(value)+SUFFIX_INDEX[suffixIndex];
        } else if (value<1000 || suffixIndex>=SUFFIX_INDEX.length) {
            return df1000.format(value)+SUFFIX_INDEX[suffixIndex];
        } else {
            return formatValue(value/1000, suffixIndex+1);
        }
    }

    private String formatValue(double value) {
        return formatValue(value, 0);
    }

    @Override
    protected String getTextAtMark(int i) {
        return formatValue(intervalStepActualValue*i);
    }

    public LinearGauge(int color, double startAngle, double endAngle, double maxSpacingAngle) {
        super(color, startAngle, endAngle, maxSpacingAngle);
    }
}
