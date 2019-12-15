package net.pregi.android.speedtester.speedtest.ui.gui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

class StepwiseGauge extends Gauge {
    /** <p></p>This is the list of interval steps.</p>
     */
    private double[] markValues;

    private double value = 0;
    public void setValue(double value) {
        this.value = value;

        int i=0;
        while (i+2<markValues.length && value>markValues[i+1]) {
            i++;
        }

        double from = markValues[i];
        double to = markValues[i+1];
        if (from<value && value<to) {
            setLevel((i+(value-from)/(to-from))/getMajorMarksCount());
        } else if (value<=from) {
            setLevel(i/getMajorMarksCount());
        } else if (to<=value) {
            setLevel((i + 1) / getMajorMarksCount());
        } else {
            setLevel(0);
        }
    }

    @Override
    protected double getValueAtMark(int i) {
        return markValues[i];
    }

    public StepwiseGauge(int color, double startAngle, double endAngle, double[] markValues) {
        super(color, startAngle, endAngle, markValues.length);

        double[] markValuesCopy = new double[markValues.length+1];
        markValuesCopy[0] = 0;
        System.arraycopy(markValues, 0, markValuesCopy, 1, markValues.length);
        this.markValues = markValuesCopy;
    }
}
