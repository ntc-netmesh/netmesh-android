package net.pregi.android.speedtester.speedtest.ui.gui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

abstract class Gauge {
    /** <p></p>This is the list of interval steps.
     * As the displayed value grows larger, the interval steps should go larger as well.</p>
     *
     * <p>The last value in the list is used instead for multiplying intervalStepValueTimes
     * before resetting the index.</p>
     */
    private static final float[] INTERVAL_STEPS_LINEAR
            = new float[] {1, 5, 10, 20, 100, 200, 1000};

    // Parameters
    /** Gauge angles. These indicate where each gauge starts and ends.
    They're expressed in degrees, and run counterclockwise from +x. */
    private double startAngle, endAngle;

    private int majorMarkLength = 30;
    private int minorMarkLength = 24;
    private int markMargin = 5;

    // working vars
    private int majorMarksCount;
    private int marksCount;
    private double startAngleR;
    private double spacingAngleR;
    private Paint majorMarkPaint, minorMarkPaint;
    private Paint markSweepPaint;

    private double value = 0, displayedValue = 0;
    public void setLevel(double value) {
        this.value = value>0 ? value<1 ? value : 1 : 0;
    }
    public abstract void setValue(double value);

    protected int getMajorMarksCount() {
        return majorMarksCount;
    }

    protected int getMarksCount() {
        return marksCount;
    }

    /** <p>Called on each frame of draw to update the value it will display.</p>
     *
     * @return whether the displayed value changes; if false, then no draw calls need to be called.
     */
    public boolean updateAnimatedValue() {
        if (value == displayedValue) {
            return false;
        } else {
            displayedValue = value+(displayedValue-value)*0.88;
            if (Math.abs(displayedValue-value)<0.0001) {
                displayedValue = value;
            }
            return true;
        }
    }

    public void reset() {
        displayedValue = value;
    }

    protected abstract String getTextAtMark(int i);

    private void drawNumbers(Canvas canvas, int centerX, int centerY, float radius, Paint textPaint) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = -fm.ascent;

        double textRadius = radius-majorMarkLength-textHeight-4;

        for (int i = 0; i <= majorMarksCount; i++) {
            double currentAngleR = startAngleR + spacingAngleR * i * 2;
            double angleX = Math.cos(currentAngleR);
            double angleY = Math.sin(currentAngleR);


            String text = getTextAtMark(i);
            float textWidth = textPaint.measureText(text);

            float x = (float) (centerX + textRadius * angleX - textWidth / 2);
            float y = (float) (centerY - textRadius * angleY + textHeight / 2);
            canvas.drawText(text, x, y, textPaint);
        }
    }

    public void drawMarks(Canvas canvas, int centerX, int centerY, float radius) {
        double majorFromRadius = radius-majorMarkLength-markMargin;
        double minorFromRadius = radius-minorMarkLength-markMargin;
        double toRadius = radius-markMargin;

        for (int i=0;i<=marksCount;i++) {
            double currentAngleR = startAngleR+spacingAngleR*i;

            double angleX = Math.cos(currentAngleR);
            double angleY = Math.sin(currentAngleR);

            if (i%2 == 0) {
                canvas.drawLine((float)(centerX+majorFromRadius*angleX),
                        (float)(centerY-majorFromRadius*angleY),
                        (float)(centerX+toRadius*angleX),
                        (float)(centerY-toRadius*angleY),
                        majorMarkPaint);
            } else {
                canvas.drawLine((float)(centerX+minorFromRadius*angleX),
                        (float)(centerY-minorFromRadius*angleY),
                        (float)(centerX+toRadius*angleX),
                        (float)(centerY-toRadius*angleY),
                        minorMarkPaint);
            }
        }
    }

    private void drawValue(Canvas canvas, int centerX, int centerY, float radius) {
        double handAngle = startAngle+(endAngle-startAngle)*displayedValue;
        double handAngleR = handAngle*Math.PI/180;

        double angleX = Math.cos(handAngleR);
        double angleY = Math.sin(handAngleR);
        double toRadius = radius-5;

        canvas.drawLine((float)(centerX),
                (float)(centerY),
                (float)(centerX+toRadius*angleX),
                (float)(centerY-toRadius*angleY),
                majorMarkPaint);

        float arcRadius = radius-markMargin-markSweepPaint.getStrokeWidth()/2;
        canvas.drawArc(new RectF(centerX-arcRadius, centerY-arcRadius, centerX+arcRadius, centerY+arcRadius), (float)-startAngle, (float)-(handAngle-startAngle), false, markSweepPaint);
    }

    public void draw(Canvas canvas, int centerX, int centerY, float radius, Paint textPaint) {
        drawValue(canvas, centerX, centerY, radius);
        drawNumbers(canvas, centerX, centerY, radius, textPaint);
    }

    public Gauge(int color, double startAngle, double endAngle, int majorSegments) {
        this(color, startAngle, endAngle);

        majorMarksCount = majorSegments;
        marksCount = majorSegments*2;
        spacingAngleR = (endAngle-startAngle)/marksCount*Math.PI/180;

        reset();
    }
    public Gauge(int color, double startAngle, double endAngle, double maxSpacingAngle) {
        this(color, startAngle, endAngle);

        majorMarksCount = (int)Math.ceil(Math.abs(endAngle-startAngle)/maxSpacingAngle);
        marksCount = majorMarksCount*2;
        spacingAngleR = (endAngle-startAngle)/marksCount*Math.PI/180;

        reset();
    }
    private Gauge(int color, double startAngle, double endAngle) {
        // Force opacity.
        color = color | 0xFF000000;

        this.startAngle = startAngle;
        this.endAngle = endAngle;

        majorMarkPaint = new Paint();
        majorMarkPaint.setColor(color);
        majorMarkPaint.setStyle(Paint.Style.STROKE);
        majorMarkPaint.setStrokeWidth(5.0f);

        minorMarkPaint = new Paint();
        minorMarkPaint.setColor(color);
        minorMarkPaint.setStyle(Paint.Style.STROKE);
        minorMarkPaint.setStrokeWidth(2.0f);

        markSweepPaint = new Paint();
        markSweepPaint.setColor(color & 0xAAFFFFFF);
        markSweepPaint.setStyle(Paint.Style.STROKE);
        markSweepPaint.setStrokeWidth(majorMarkLength);

        startAngleR = startAngle*Math.PI/180;
    }
}
