package net.pregi.android.speedtester.speedtest.ui.gui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import net.pregi.networking.speedtest.IOProcessMode;

import java.text.NumberFormat;

public class SpeedtestGraphicDisplay {
    /** This is the fraction of the width or height (whichever is smaller)
     *  the circle's diameter should span.
     */

    //// Parameter variables

    private int colorBackground = 0xFF000000;
    private int colorCircle = 0xFF888888;

    private int downloadColor = 0xFF00FFFF;
    private int uploadColor = 0xFFFF8800;
    private int pingColor = 0xFF008800;

    private Gauge downloadGauge = new LinearGauge(downloadColor, 215, 100, 20);
    private Gauge uploadGauge = new LinearGauge(uploadColor, -35, 80, 20);
    private Gauge pingGauge = new StepwiseGauge(pingColor, -125, -55, new double[] {1, 10, 100, 1000} );

    //// Working variables


    private SurfaceHolder surfaceHolder;
    private boolean ready = false;

    /** This is the drawable that would comprise the (relatively static) background. */
    private Bitmap backgroundImage;
    /** This is the drawable that would comprise the (relatively static) foreground. */
    private Bitmap badgeImage;

    private int width;
    private int height;
    private int centerX, centerY;
    private int bitmapSquareSize;
    private int badgeBitmapSquareSize;
    /** This is the coordinate of the bitmap's top-left relative to the canvas. */
    private int bitmapX, bitmapY;
    /** This is the coordinate of the bitmap's center relative to itself. */
    private int bitmapCX, bitmapCY;
    private float radius;
    private RectF circleRect; // for drawing the outer circle bounded by this rect.
    private float displayDensity;

    private Paint gaugeNumberPaint, textPaint;
    private Paint paintCircleOuter, paintCircleInner, paintCircleInnerFill;
    private Paint ioProgressPaint;

    private static final String[] SCALE_PREFIX = new String[] {"", "K", "M", "G", "T", "P"};
    private double throughputPlaceValueFactor = 1;
    private int throughputPrefixIndex = 0;
    private float badgeNumberTextSize;
    private float badgeLabelTextSize;

    private String throughputUnitsText = "bps";
    private float throughputUnitsTextY;

    private double downloadSpeed;
    private String downloadSpeedText = "";
    private float downloadSpeedTextY;
    public void setDownloadSpeed(double value) {
        downloadGauge.setValue(downloadSpeed=value*8); // show bits, not bytes
        renderThroughputNumbers();
    }

    private double uploadSpeed;
    private String uploadSpeedText = "";
    private float uploadSpeedTextY;
    public void setUploadSpeed(double value) {
        uploadGauge.setValue(uploadSpeed=value*8); // show bits, not bytes
        renderThroughputNumbers();
    }

    private double ping;
    private String pingText = "";
    private float pingTextAlignX;
    private float pingTextY;
    private float pingLabelWidth;
    public void setPing(double value) {
        pingGauge.setValue(ping=value);
        pingText = renderNumber(ping);
    }

    private double ioProgress = 0; // percent of download/upload
    private double ioProgressDisplayed = 0, ioProgressDisplayedPrevious = 0;
    private IOProcessMode ioProgressMode;
    public void setIOProgress(double value) {
        ioProgress = value;
    }
    public void setIOProgress(double value, IOProcessMode mode) {
        ioProgress = value;
        if (ioProgressMode != mode) {
            if (mode == IOProcessMode.DOWNLOAD) {
                ioProgressPaint.setColor(downloadColor);
            } else if (mode == IOProcessMode.UPLOAD) {
                ioProgressPaint.setColor(uploadColor);
            } else if (mode == IOProcessMode.PING) {
                ioProgressPaint.setColor(pingColor);
            } else {
                ioProgressPaint.setColor(0xFFFFFFFF);
            }
        }
        ioProgressMode = mode;
    }
    public void clearIOProgress() {
        // implemented in case there will be different representations of "0%" and "done".
        ioProgress = 0;
        ioProgressDisplayed = 0;
        setIOProgress(0, null);
    }
    public double getIOProgress() {
        return ioProgress;
    }

    private NumberFormat nf100;

    private String renderNumber(double value) {
        if (value<1000) {
            String output = nf100.format(value);
            int dotIndex = output.indexOf('.');
            if (output.length()>4) {
                if (dotIndex != -1 && dotIndex < 4) {
                    return output.substring(0, 5);
                } else {
                    return output.substring(0, 4);
                }
            } else {
                return output;
            }
        } else if (value<10000){
            return Integer.toString((int)Math.floor(value));
        } else {
            return ((int)Math.floor(value))+"K";
        }
    }

    private void renderThroughputNumbers() {
        // adjust the place value factor.
        throughputPlaceValueFactor = 1;
        throughputPrefixIndex = 0;
        while (downloadSpeed>throughputPlaceValueFactor*1000 && throughputPrefixIndex<SCALE_PREFIX.length-1) {
            throughputPlaceValueFactor *= 1000;
            throughputPrefixIndex++;
        }
        while (uploadSpeed>throughputPlaceValueFactor*1000 && throughputPrefixIndex<SCALE_PREFIX.length-1) {
            throughputPlaceValueFactor *= 1000;
            throughputPrefixIndex++;
        }

        downloadSpeedText = renderNumber(downloadSpeed/ throughputPlaceValueFactor);
        uploadSpeedText = renderNumber(uploadSpeed/ throughputPlaceValueFactor);

        throughputUnitsText = SCALE_PREFIX[throughputPrefixIndex]+"bps";
    }

    public void reset() {
        setPing(0);
        setDownloadSpeed(0);
        setUploadSpeed(0);

        pingGauge.reset();
        downloadGauge.reset();
        uploadGauge.reset();

        renderThroughputNumbers();
    }

    private void redrawBackground() {
        Canvas canvas = new Canvas(backgroundImage);
        // draw the background color.
        canvas.drawColor(colorBackground);
        // draw the circle
        canvas.drawCircle(bitmapCX, bitmapCY, radius, paintCircleOuter);

        // Draw the gauges.
        downloadGauge.drawMarks(canvas, bitmapCX, bitmapCY, radius);
        uploadGauge.drawMarks(canvas, bitmapCX, bitmapCY, radius);
        pingGauge.drawMarks(canvas, bitmapCX, bitmapCY, radius);

        draw(true);
    }

    private void drawTextCentered(Canvas canvas, String text, float cx, float y, Paint paint) {
        canvas.drawText(text, cx-paint.measureText(text)/2, y, paint);
    }

    private boolean draw(boolean initialize) {
        if (!ready) {
            return false;
        }

        ioProgressDisplayed = ioProgress+(ioProgressDisplayed-ioProgress)*0.3;
        if (Math.abs(ioProgressDisplayed-ioProgress)<0.0001) {
            ioProgressDisplayed = ioProgress;
        }
        boolean ioProgressUpdated = ioProgressDisplayedPrevious != ioProgressDisplayed;
        ioProgressDisplayedPrevious = ioProgressDisplayed;

        // Check if the displayed graphic will be modified.
        boolean downloadGaugeUpdated = downloadGauge.updateAnimatedValue();
        boolean uploadGaugeUpdated = uploadGauge.updateAnimatedValue();
        boolean pingGaugeUpdated = pingGauge.updateAnimatedValue();
        if (!downloadGaugeUpdated && !uploadGaugeUpdated && !pingGaugeUpdated && !ioProgressUpdated && !initialize) {
            return true;
        }

        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) {
            return false;
        }

        if (initialize) {
            canvas.drawColor(colorBackground);
        }

        // Draw the background.
        canvas.drawBitmap(backgroundImage, bitmapX, bitmapY, null);

        // Draw the progress bar
        canvas.drawArc(circleRect, (float)(-90), (float)(ioProgressDisplayed*360), false, ioProgressPaint);


        // Draw the gauges
        downloadGauge.draw(canvas, centerX, centerY, radius, gaugeNumberPaint);
        uploadGauge.draw(canvas, centerX, centerY, radius, gaugeNumberPaint);
        pingGauge.draw(canvas, centerX, centerY, radius, gaugeNumberPaint);

        // Draw the center badge.
        {
            canvas.drawBitmap(badgeImage, centerX-badgeBitmapSquareSize/2, centerY-badgeBitmapSquareSize/2, null);

            textPaint.setTextSize(badgeNumberTextSize);
            textPaint.setColor(downloadColor);
            drawTextCentered(canvas, downloadSpeedText, centerX, downloadSpeedTextY, textPaint);

            textPaint.setTextSize(badgeLabelTextSize);
            textPaint.setColor(0xFFFFFFFF);
            drawTextCentered(canvas, throughputUnitsText, centerX, throughputUnitsTextY, textPaint);

            textPaint.setTextSize(badgeNumberTextSize);
            textPaint.setColor(uploadColor);
            drawTextCentered(canvas, uploadSpeedText, centerX, uploadSpeedTextY, textPaint);

            textPaint.setTextSize(badgeNumberTextSize);
            textPaint.setColor(pingColor);
            float pingTextWidth = textPaint.measureText(pingText);
            canvas.drawText(pingText, pingTextAlignX-pingTextWidth, pingTextY, textPaint);
        }

        surfaceHolder.unlockCanvasAndPost(canvas);

        return true;
    }

    public boolean draw() {
        return draw(false);
    }

    public SpeedtestGraphicDisplay(SurfaceView surfaceView, final Runnable onReady) {
        nf100 = NumberFormat.getNumberInstance();
        nf100.setMaximumFractionDigits(3);

        paintCircleOuter = new Paint();
        paintCircleOuter.setColor(colorCircle);
        paintCircleOuter.setStyle(Paint.Style.STROKE);
        paintCircleOuter.setStrokeWidth(5.0f);

        ioProgressPaint = new Paint();
        ioProgressPaint.setColor(Color.WHITE);
        ioProgressPaint.setStyle(Paint.Style.STROKE);
        ioProgressPaint.setStrokeWidth(7.0f);

        paintCircleInner = new Paint();
        paintCircleInner.setColor(colorCircle);
        paintCircleInner.setStyle(Paint.Style.STROKE);
        paintCircleInner.setStrokeWidth(12.0f);

        paintCircleInnerFill = new Paint();
        paintCircleInnerFill.setColor(colorBackground);

        Typeface typeface = Typeface.create("Helvetica", Typeface.BOLD);
        displayDensity = surfaceView.getContext().getResources().getDisplayMetrics().density;

        gaugeNumberPaint = new Paint();
        gaugeNumberPaint.setTypeface(typeface);
        gaugeNumberPaint.setTextSize(14*displayDensity+0.5f);
        gaugeNumberPaint.setColor(0xFFFFFFFF);

        textPaint = new Paint();
        textPaint.setTypeface(typeface);
        // other parameters to be set as they are needed, since they vary

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            boolean onReadyCalled = false;

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                ready = true;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                SpeedtestGraphicDisplay.this.width = width;
                SpeedtestGraphicDisplay.this.height = height;
                centerX = width/2;
                centerY = height/2;
                bitmapSquareSize = Math.min(width, height);
                bitmapX = centerX-bitmapSquareSize/2;
                bitmapY = centerY-bitmapSquareSize/2;
                bitmapCX = centerX-bitmapX;
                bitmapCY = centerY-bitmapY;
                radius = bitmapSquareSize/2.0f-10;
                circleRect = new RectF(centerX-radius, centerY-radius, centerX+radius, centerY+radius);

                // Change the gauge text size.
                gaugeNumberPaint.setTextSize(16*displayDensity*radius/250+0.5f);

                // (re)create the background drawable.
                if (backgroundImage != null) {
                    backgroundImage.recycle();
                }
                backgroundImage = Bitmap.createBitmap(bitmapSquareSize, bitmapSquareSize, Bitmap.Config.ARGB_8888);

                float badgeRadius = radius * 0.55f;
                badgeBitmapSquareSize = (int)Math.ceil(badgeRadius+paintCircleInner.getStrokeWidth()+1)*2;
                if (badgeImage != null) {
                    badgeImage.recycle();
                }
                badgeImage = Bitmap.createBitmap(badgeBitmapSquareSize, badgeBitmapSquareSize, Bitmap.Config.ARGB_8888);
                {
                    Canvas canvas = new Canvas(badgeImage);
                    float center = badgeBitmapSquareSize/2.0f;
                    float badgeBitmapX = centerX-badgeBitmapSquareSize/2.0f;
                    float badgeBitmapY = centerY-badgeBitmapSquareSize/2.0f;

                    canvas.drawCircle(center, center, badgeRadius, paintCircleInnerFill);
                    canvas.drawCircle(center, center, badgeRadius, paintCircleInner);

                    badgeLabelTextSize = badgeBitmapSquareSize*0.06f;
                    badgeNumberTextSize = badgeBitmapSquareSize*0.175f;
                    float margin = 2;
                    float runningY;
                    Paint.FontMetrics fm;

                    // draw the DOWNLOAD label.
                    textPaint.setColor(downloadColor);
                    textPaint.setTextSize(badgeLabelTextSize);
                    fm = textPaint.getFontMetrics();
                    // store this value so we don't need getFontMetrics() a lot.
                    float labelTextSize = -fm.ascent;
                    runningY = badgeBitmapSquareSize*0.1f+labelTextSize;
                    drawTextCentered(canvas, "DOWNLOAD", center, runningY, textPaint);

                    // position the DOWNLOAD value.
                    textPaint.setTextSize(badgeNumberTextSize);
                    fm = textPaint.getFontMetrics();
                    // store this value so we don't need getFontMetrics() a lot.
                    float numberTextSize = -fm.ascent;
                    runningY += numberTextSize+margin;
                    downloadSpeedTextY = badgeBitmapY+runningY;

                    // position the throughput units.
                    textPaint.setTextSize(badgeLabelTextSize);
                    runningY += labelTextSize+margin;
                    throughputUnitsTextY = badgeBitmapY+runningY;

                    // position the UPLOAD value.
                    runningY += numberTextSize+margin;
                    uploadSpeedTextY = badgeBitmapY+runningY;

                    // draw the UPLOAD value.
                    textPaint.setColor(uploadColor);
                    textPaint.setTextSize(badgeLabelTextSize);
                    runningY += labelTextSize+margin;
                    drawTextCentered(canvas, "UPLOAD", center, runningY, textPaint);

                    // For the PING, we work from the bottom of the image.
                    runningY = badgeBitmapSquareSize*0.8f;
                    float runningX = badgeBitmapSquareSize*0.66f;
                    pingTextAlignX = badgeBitmapX+runningX;
                    pingTextY = badgeBitmapY+runningY;
                    textPaint.setColor(0xFFFFFFFF);
                    canvas.drawText("ms", runningX+margin*2, runningY, textPaint);
                    textPaint.setColor(pingColor);
                    canvas.drawText("PING", runningX+margin*2, runningY-labelTextSize*1.04f, textPaint);
                }

                redrawBackground();
                if (!onReadyCalled) {
                    onReadyCalled = true;
                    if (onReady != null) {
                        onReady.run();
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                ready = false;

                if (backgroundImage != null) {
                    backgroundImage.recycle();
                    backgroundImage = null;
                }
                if (badgeImage != null) {
                    badgeImage.recycle();
                    badgeImage = null;
                }
            }
        });
    }
    public SpeedtestGraphicDisplay(SurfaceView surfaceView) {
        this(surfaceView, null);
    }
}
