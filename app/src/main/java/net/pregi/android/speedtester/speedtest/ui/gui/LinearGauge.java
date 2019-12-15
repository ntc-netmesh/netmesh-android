package net.pregi.android.speedtester.speedtest.ui.gui;

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
    private float intervalStepValueTimes = 1000;
    private float intervalStepActualValue = 1000;
    private float maximumDisplayableValue;

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
        intervalStepValueTimes = 1000;

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


    @Override
    protected double getValueAtMark(int i) {
        return intervalStepActualValue*i;
    }

    public LinearGauge(int color, double startAngle, double endAngle, double maxSpacingAngle) {
        super(color, startAngle, endAngle, maxSpacingAngle);
    }
}
