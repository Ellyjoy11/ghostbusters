package com.motorola.ghostbusters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by elenalast on 6/5/15.
 */
@SuppressLint({ "DrawAllocation", "ClickableViewAccessibility" })
public class MyChart extends View {

    public static final String TAG = "Ghostbusters";
    Paint paintAxis, paintValues;
    Paint paintThresholds;
    Paint paintLimits, paintHyst;
    Paint paintText, paintSwipes;

    float axisX1, axisX2, axisX_Y;
    float axisY_X, axisY1, axisY2;
    float xCenter;
    float yDrawSwipes;
    public int axisPad = 20;
    public int mTextSize;
    float xSize;
    float xStep;
    float paintWidth;

    public static int threshold;
    public static int satCap;
    public static int hysteresis;

    public static float xZeroLine;
    public static float xScale;
    public static float upperLine;

    int realScreenHeight;
    int screenWidth;
    int screenHeight;

    public static int[] mMax;
    public static int[] mMin;

    public static int swipeIndex;

    public MyChart(Context context) {
        super(context);
        initMyView();
    }

    public MyChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMyView();
    }

    public MyChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initMyView();
    }

    public void initMyView() {

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = ((Activity) getContext()).getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;

        mMax  = new int[MainActivity.gearsCount+1];
        mMin = new int[MainActivity.gearsCount+1];

        if (screenHeight < 1280) {
            //axisPad = 25;
            mTextSize = 25;
            realScreenHeight = 1280;
        } else if (screenHeight < 1920) {
            //axisPad = 50;
            mTextSize = 50;
            realScreenHeight = 1920;
        } else {
            //axisPad = 50;
            mTextSize = 60;
            realScreenHeight = 2560;
        }

        axisX1 = axisPad - 5;
        axisX2 = screenWidth - axisPad;
        axisX_Y = 4 * screenHeight / 10;
        xCenter = (axisX2 - axisX1)/2;

        xSize = axisX2 - axisX1;
        xStep = xSize / (MainActivity.gearsCount + 2);
        paintWidth = xStep / 3;

        axisY1 = axisPad - 5;
        axisY2 = 8 * screenHeight / 10;
        axisY_X = axisX1;

        paintValues = new Paint();
        paintValues.setColor(Color.BLUE);
        paintValues.setStrokeWidth(paintWidth);
        paintValues.setStyle(Paint.Style.FILL_AND_STROKE);

        paintAxis = new Paint();
        paintAxis.setColor(Color.BLACK);
        paintAxis.setStrokeWidth(3);
        paintAxis.setStyle(Paint.Style.FILL_AND_STROKE);

        paintThresholds = new Paint();
        paintThresholds.setColor(Color.GREEN);
        paintThresholds.setStrokeWidth(5);
        paintThresholds.setStyle(Paint.Style.FILL_AND_STROKE);

        paintLimits = new Paint();
        paintLimits.setColor(Color.RED);
        paintLimits.setStrokeWidth(5);
        paintLimits.setStyle(Paint.Style.FILL_AND_STROKE);

        paintHyst = new Paint();
        paintHyst.setColor(Color.parseColor("#FF8300"));
        paintHyst.setStrokeWidth(5);
        paintHyst.setStyle(Paint.Style.FILL_AND_STROKE);

        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(mTextSize);

        paintText.setStrokeWidth(2);
        paintText.setStyle(Paint.Style.FILL_AND_STROKE);

        paintSwipes = new Paint();
        paintSwipes.setColor(Color.GRAY);
        paintSwipes.setStrokeWidth(3);
        paintSwipes.setStyle(Paint.Style.STROKE);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawLine(axisX1, axisX_Y, axisX2, axisX_Y, paintAxis);
        canvas.drawLine(axisY_X, axisY1, axisY_X, axisY2, paintAxis);

        upperLine = (float) getMaxToDraw(threshold, mMax, mMin);
        yDrawSwipes = axisY1 + 4*axisPad + 2*mTextSize;
        //Log.d(TAG, "upper line is " + upperLine);
        xZeroLine = axisX_Y;
        //Log.d(TAG, "zero line is " + xZeroLine + "; topY " + axisY1);
        xScale = upperLine / (xZeroLine - axisY1 - 8 * axisPad);
        //Log.d(TAG, "scale factor is " + xScale);

        for (int i=1; i <= MainActivity.gearsCount+1; i++) {
            String gearName = "";
            if (i == MainActivity.gearsCount+1) {
                gearName = "auto";
                paintValues.setColor(Color.parseColor("#027f3d"));
                paintText.setColor(Color.parseColor("#027f3d"));
            } else {
                gearName = Integer.toString(i - 1);
                paintValues.setColor(Color.BLUE);
                paintText.setColor(Color.BLUE);
            }
            if ((i < MainActivity.gearsCount+1 && Character.toString(MainActivity.gearsEnabled[i-1]).equals("1")) || i == MainActivity.gearsCount+1) {
                canvas.drawLine(axisX1 + i * xStep, xZeroLine + mMin[i - 1] / xScale, axisX1 + i * xStep, xZeroLine - mMax[i - 1] / xScale, paintValues);
                canvas.drawText(Integer.toString(-1 * mMin[i - 1]), axisX1 + i * xStep - 5 * mTextSize / 10, axisY2 - 2*mTextSize, paintText);
                canvas.drawText(Integer.toString(mMax[i - 1]), axisX1 + i * xStep - 5 * mTextSize / 10, axisY1 + axisPad + 2* mTextSize, paintText);
            }
            paintText.setColor(Color.BLACK);
            canvas.drawText(gearName, axisX1 + i * xStep - mTextSize, xZeroLine - mTextSize / 3, paintText);
           // Log.d(TAG, "drawing values");
        }

        paintText.setColor(Color.BLACK);
        if (satCap < upperLine) {
            //draw saturation if it's less than maximum values
            canvas.drawLine(axisX1, xZeroLine - satCap/xScale, axisX2, xZeroLine - satCap/xScale, paintLimits);
            canvas.drawText(Integer.toString(satCap), axisX1 + mTextSize - axisPad, xZeroLine - satCap/xScale - 7*mTextSize/10, paintText);
        }
        //draw hysteresis
        canvas.drawLine(axisX1, xZeroLine - hysteresis / xScale, axisX2, xZeroLine - hysteresis / xScale, paintHyst);
        canvas.drawText(Integer.toString(hysteresis), axisX1 + mTextSize - 2*axisPad, xZeroLine - hysteresis / xScale + mTextSize, paintText);
        canvas.drawLine(axisX1, xZeroLine + hysteresis / xScale, axisX2, xZeroLine + hysteresis / xScale, paintHyst);
        canvas.drawText(Integer.toString(-1 * hysteresis), axisX1 + mTextSize - 2*axisPad, xZeroLine + hysteresis / xScale + mTextSize, paintText);

        //draw thresholds
        canvas.drawLine(axisX1, xZeroLine - threshold / xScale, axisX2, xZeroLine - threshold / xScale, paintThresholds);
        canvas.drawText(Integer.toString(threshold), axisX1 + mTextSize - 2*axisPad, xZeroLine - threshold/xScale - mTextSize / 3, paintText);
        canvas.drawLine(axisX1, xZeroLine + threshold / xScale, axisX2, xZeroLine + threshold / xScale, paintThresholds);
        canvas.drawText(Integer.toString(-1 * threshold), axisX1 + mTextSize - 2*axisPad, xZeroLine + threshold / xScale - mTextSize / 3, paintText);

        for (int j=0; j < MainActivity.TEST_CYCLES; j++) {
            if (j == swipeIndex) {
                paintSwipes.setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                paintSwipes.setStyle(Paint.Style.STROKE);
            }
            canvas.drawCircle(xCenter - 50 * SlideShow.intTimeRange + 50 * j, yDrawSwipes, 15, paintSwipes);
        }

    }

    public static void setThresholds(int thresh, int satC, int hyst) {

        threshold = thresh;
        satCap = satC;
        hysteresis = hyst;
    }

    public static void setArrays (int[] mmin, int[] mmax) {
        mMax = mmax;
        mMin = mmin;
        //Log.d(TAG, "trying to set arrays");
    }

    public static void setSwipeProgress (int index) {
        swipeIndex = index;
    }

    public static int getMaxToDraw(int upThresh, int[] maxPlus, int[] maxMinus) {
        int maxToDraw = upThresh;

        for (int i=0; i < maxPlus.length-1; i++){
            //Log.d (TAG, "array[" + i + "] = " + maxPlus[i]);
            if (maxPlus[i] > maxToDraw && Character.toString(MainActivity.gearsEnabled[i]).equals("1")) {
                maxToDraw = maxPlus[i];
            }
        }
        for (int i=0; i < maxMinus.length-1; i++){
            //Log.d (TAG, "array[" + i + "] = " + maxMinus[i]);
            if (maxMinus[i] > maxToDraw && Character.toString(MainActivity.gearsEnabled[i]).equals("1")) {
                maxToDraw = maxMinus[i];
            }
        }
        if (maxPlus[maxPlus.length-1] > maxToDraw) {
            maxToDraw = maxPlus[maxPlus.length-1];
        }
        if (maxMinus[maxMinus.length-1] > maxToDraw) {
            maxToDraw = maxMinus[maxMinus.length-1];
        }

        //Log.d(TAG, "max to draw " + maxToDraw);
        return maxToDraw;

    }

}
