package net.pregi.android.speedtester.speedtest.ui.gui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

class StepwiseGauge extends Gauge {
    /** <p></p>This is the list of interval steps.</p>
     */
    private double[] markValues;

    private DecimalFormat df10 = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.getDefault()));
    private DecimalFormat df1000 = new DecimalFormat("#", new DecimalFormatSymbols(Locale.getDefault()));

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
        return formatValue(markValues[i]);
    }

    public StepwiseGauge(int color, double startAngle, double endAngle, double[] markValues) {
        super(color, startAngle, endAngle, markValues.length);

        double[] markValuesCopy = new double[markValues.length+1];
        markValuesCopy[0] = 0;
        System.arraycopy(markValues, 0, markValuesCopy, 1, markValues.length);
        this.markValues = markValuesCopy;
    }
}
