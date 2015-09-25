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
    Paint paintText;

    float axisX1, axisX2, axisX_Y;
    float axisY_X, axisY1, axisY2;
    public int axisPad = 20;
    public int mTextSize;
    //public int gearsNum;
    float xSize;
    float xStep;
    float paintWidth;
    //float step;
    //public static int mProgress = 0;
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

    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawLine(axisX1, axisX_Y, axisX2, axisX_Y, paintAxis);
        canvas.drawLine(axisY_X, axisY1, axisY_X, axisY2, paintAxis);

        upperLine = (float) getMaxToDraw(threshold, mMax, mMin);
        //upperLine = (float)25;

        //upperLine = (float) 730;
        //Log.d(TAG, "upper line is " + upperLine);
        xZeroLine = axisX_Y;
        //Log.d(TAG, "zero line is " + xZeroLine + "; topY " + axisY1);
        xScale = upperLine / (xZeroLine - axisY1 - 5*axisPad);
        //Log.d(TAG, "scale factor is " + xScale);
        //int[] val = {730, 200, 45, 280, 400, 678, 456, 350};

        if (satCap < upperLine) {
            //draw saturation if it's less than maximum values
            canvas.drawLine(axisX1, xZeroLine - satCap/xScale, axisX2, xZeroLine - satCap/xScale, paintLimits);
            canvas.drawText(Integer.toString(satCap), axisX1 + mTextSize - axisPad, xZeroLine - satCap/xScale - 7*mTextSize/10, paintText);
        }
        //draw hysteresis
        canvas.drawLine(axisX1, xZeroLine - hysteresis / xScale, axisX2, xZeroLine - hysteresis / xScale, paintHyst);
        canvas.drawText(Integer.toString(hysteresis), axisX1 + mTextSize - axisPad, xZeroLine - hysteresis / xScale - 7*mTextSize/10, paintText);
        canvas.drawLine(axisX1, xZeroLine + hysteresis / xScale, axisX2, xZeroLine + hysteresis / xScale, paintHyst);
        canvas.drawText(Integer.toString(-1 * hysteresis), axisX1 + mTextSize - axisPad, xZeroLine + hysteresis / xScale - 7 * mTextSize / 10, paintText);

        //draw thresholds
        canvas.drawLine(axisX1, xZeroLine - threshold / xScale, axisX2, xZeroLine - threshold / xScale, paintThresholds);
        canvas.drawText(Integer.toString(threshold), axisX1 + mTextSize - axisPad, xZeroLine - threshold/xScale + 17 * mTextSize/10, paintText);
        canvas.drawLine(axisX1, xZeroLine + threshold / xScale, axisX2, xZeroLine + threshold / xScale, paintThresholds);
        canvas.drawText(Integer.toString(-1 * threshold), axisX1 + mTextSize - axisPad, xZeroLine + threshold / xScale - 7 * mTextSize / 10, paintText);

        for (int i=1; i <= MainActivity.gearsCount+1; i++) {
            String gearName = "";
            if (i == MainActivity.gearsCount+1) {
                gearName = "auto";
                paintValues.setColor(Color.parseColor("#027f3d"));
            } else {
                gearName = Integer.toString(i - 1);
                paintValues.setColor(Color.BLUE);
            }
            canvas.drawLine(axisX1 + i * xStep, xZeroLine + mMin[i-1]/xScale, axisX1 + i * xStep, xZeroLine - mMax[i-1]/xScale, paintValues);
            canvas.drawText(gearName, axisX1 + i * xStep - mTextSize, xZeroLine - mTextSize / 6, paintText);
            canvas.drawText(Integer.toString(-1*mMin[i-1]), axisX1 + i * xStep - 5*mTextSize/10, axisY2 - mTextSize, paintText);
            canvas.drawText(Integer.toString(mMax[i-1]), axisX1 + i * xStep - 5*mTextSize/10, axisY1 +axisPad+ mTextSize, paintText);
           // Log.d(TAG, "drawing values");
        }
/*
        String textToShowAtBottom = "Product: " + MainActivity.productInfo + "  Config: " + MainActivity.getTouchCfg();
        if (!MainActivity.panel.isEmpty()) {
            textToShowAtBottom += " Panel: " + MainActivity.panel;
        }
        Rect areaRect = new Rect(0,88 * screenHeight / 100 - 3*mTextSize, screenWidth, 88 * screenHeight / 100 - mTextSize);
        RectF bounds = new RectF(areaRect);
        bounds.right = paintText.measureText(textToShowAtBottom, 0, textToShowAtBottom.length());
        bounds.bottom = paintText.descent() - paintText.ascent();

        bounds.left += (areaRect.width() - bounds.right) / 2.0f;
        bounds.top += (areaRect.height() - bounds.bottom) / 2.0f;

        canvas.drawText(textToShowAtBottom, bounds.left, bounds.top - paintText.ascent(), paintText);
        */
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

    public static int getMaxToDraw(int upThresh, int[] maxPlus, int[] maxMinus) {
        int maxToDraw = upThresh;

        for (int i=0; i < maxPlus.length; i++){
            //Log.d (TAG, "array[" + i + "] = " + maxPlus[i]);
            if (maxPlus[i] > maxToDraw) {
                maxToDraw = maxPlus[i];
            }
        }
        for (int i=0; i < maxMinus.length; i++){
            //Log.d (TAG, "array[" + i + "] = " + maxMinus[i]);
            if (maxMinus[i] > maxToDraw) {
                maxToDraw = maxMinus[i];
            }
        }
        //Log.d(TAG, "max to draw " + maxToDraw);
        return maxToDraw;

    }

}
