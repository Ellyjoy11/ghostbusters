package com.motorola.ghostbusters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by elenalast on 6/5/15.
 */
@SuppressLint({ "DrawAllocation", "ClickableViewAccessibility" })
public class MyAbsChart extends View {

    public static final String TAG = "Ghostbusters";
    Paint paintAxis, paintValues;
    Paint paintThresholds;
    Paint paintText, paintSwipes;

    float axisX1, axisX2, axisX_Y;
    float axisY_X, axisY1, axisY2;
    float xCenter;
    float yDrawSwipes;
    public int axisPad = 20;
    public int mTextSize;
    //public int gearsNum;
    float xSize;
    float xStep;
    float paintWidth;
    float shiftSize;
    //float step;
    //public static int mProgress = 0;
    public static int mTxThreshold;
    public static int mRxThreshold;

    public static float xZeroLine;
    public static float xScale;
    public static float upperLine;
    int realScreenHeight;
    int screenWidth;
    int screenHeight;
    public static int[] mRxMax;
    public static int[] mRxMin;
    public static int[] mTxMax;
    public static int[] mTxMin;

    public static int swipeIndex;

    public MyAbsChart(Context context) {
        super(context);
        initMyView();
    }

    public MyAbsChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMyView();
    }

    public MyAbsChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initMyView();
    }

    public void initMyView() {

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = ((Activity) getContext()).getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;

        mRxMax  = new int[MainActivity.stretches];
        mRxMin = new int[MainActivity.stretches];
        mTxMax  = new int[MainActivity.stretches];
        mTxMin = new int[MainActivity.stretches];

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
        xStep = xSize / (MainActivity.stretches + 2);
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
        paintThresholds.setStrokeWidth(5);
        paintThresholds.setStyle(Paint.Style.FILL_AND_STROKE);

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

        if (MainActivity.isRxEnabled && MainActivity.isTxEnabled) {
            paintWidth = xStep / 4;
            shiftSize = (float) Math.ceil(paintWidth/1.55);
        } else {
            shiftSize = 0;
        }
        Log.d(TAG, "shift: " + shiftSize);

        upperLine = (float) getMaxToDraw(mTxThreshold, mRxThreshold, mTxMax, mTxMin, mRxMax, mRxMin);
        yDrawSwipes = axisY1 + 4*axisPad + 2*mTextSize;

        //Log.d(TAG, "upper line is " + upperLine);
        xZeroLine = axisX_Y;
        //Log.d(TAG, "zero line is " + xZeroLine + "; topY " + axisY1);
        xScale = upperLine / (xZeroLine - axisY1 - 8 * axisPad);

        //draw Tx thresholds
        if (MainActivity.isTxEnabled) {
            paintThresholds.setColor(Color.parseColor("#db5da9"));
            paintText.setColor(Color.parseColor("#db5da9"));
            canvas.drawLine(axisX1, xZeroLine - mTxThreshold / xScale, axisX2, xZeroLine - mTxThreshold / xScale, paintThresholds);
            canvas.drawText(Integer.toString(mTxThreshold), axisX1 + mTextSize - axisPad/2, xZeroLine - mTxThreshold / xScale - mTextSize / 3 - 2*axisPad, paintText);
            canvas.drawText("Tx", axisX2 - mTextSize - axisPad, xZeroLine + mTxThreshold / xScale + mTextSize, paintText);
            canvas.drawLine(axisX1, xZeroLine + mTxThreshold / xScale, axisX2, xZeroLine + mTxThreshold / xScale, paintThresholds);
            canvas.drawText(Integer.toString(-1 * mTxThreshold), axisX1 + mTextSize - axisPad/2, xZeroLine + mTxThreshold / xScale + mTextSize + 2*axisPad, paintText);
        }
        //draw Rx thresholds
        if (MainActivity.isRxEnabled) {
            paintThresholds.setColor(Color.parseColor("#11e7ac"));
            paintText.setColor(Color.parseColor("#11e7ac"));
            canvas.drawLine(axisX1, xZeroLine - mRxThreshold / xScale, axisX2, xZeroLine - mRxThreshold / xScale, paintThresholds);
            canvas.drawText(Integer.toString(mRxThreshold), axisX1 + mTextSize - 2*axisPad, xZeroLine - mRxThreshold / xScale - mTextSize / 3, paintText);
            canvas.drawText("Rx", axisX2 - mTextSize - axisPad, xZeroLine - mRxThreshold / xScale - mTextSize / 3, paintText);
            canvas.drawLine(axisX1, xZeroLine + mRxThreshold / xScale, axisX2, xZeroLine + mRxThreshold / xScale, paintThresholds);
            canvas.drawText(Integer.toString(-1 * mRxThreshold), axisX1 + mTextSize - 2*axisPad, xZeroLine + mRxThreshold / xScale + mTextSize, paintText);
        }

        for (int i = 1; i <= MainActivity.stretches; i++) {
            String gearName = Integer.toString(MainActivity.baseStretch + i - 1);
            if (MainActivity.isRxEnabled) {

                    paintValues.setColor(Color.parseColor("#11e7ac"));
                    paintText.setColor(Color.parseColor("#11e7ac"));

                Log.d(TAG, "drawing stretch " + gearName);
                canvas.drawLine(axisX1 + i * xStep - shiftSize, xZeroLine + mRxMin[i - 1] / xScale, axisX1 + i * xStep - shiftSize, xZeroLine - mRxMax[i - 1] / xScale, paintValues);
                canvas.drawText(Integer.toString(-1 * mRxMin[i - 1]), axisX1 + i * xStep - 5 * mTextSize / 10 - shiftSize, axisY2 - 2*mTextSize, paintText);
                canvas.drawText(Integer.toString(mRxMax[i - 1]), axisX1 + i * xStep - 5 * mTextSize / 10 - shiftSize, axisY1 + axisPad + 2*mTextSize, paintText);
            }
            if (MainActivity.isTxEnabled) {

                    paintValues.setColor(Color.parseColor("#db5da9"));
                    paintText.setColor(Color.parseColor("#db5da9"));

                Log.d(TAG, "drawing stretch " + gearName);
                canvas.drawLine(axisX1 + i * xStep + shiftSize, xZeroLine + mTxMin[i - 1] / xScale, axisX1 + i * xStep + shiftSize, xZeroLine - mTxMax[i - 1] / xScale, paintValues);
                canvas.drawText(Integer.toString(-1 * mTxMin[i - 1]), axisX1 + i * xStep - 5 * mTextSize / 10 + shiftSize, axisY2 - mTextSize, paintText);
                canvas.drawText(Integer.toString(mTxMax[i - 1]), axisX1 + i * xStep - 5 * mTextSize / 10 + shiftSize, axisY1 + axisPad + mTextSize, paintText);
            }
            paintText.setColor(Color.BLACK);
            canvas.drawText(gearName, axisX1 + i * xStep - mTextSize, xZeroLine - mTextSize / 3, paintText);

        }
        paintText.setColor(Color.BLACK);

        for (int j=0; j < MainActivity.TEST_CYCLES; j++) {
            if (j == swipeIndex) {
                paintSwipes.setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                paintSwipes.setStyle(Paint.Style.STROKE);
            }
            canvas.drawCircle(xCenter - 50 * SlideShow.intTimeRange + 50 * j, yDrawSwipes, 15, paintSwipes);
        }

    }

    public static void setArrays (int[] rxMin, int[] rxMax, int[] txMin, int[] txMax) {
        mRxMax = rxMax;
        mRxMin = rxMin;
        mTxMax = txMax;
        mTxMin = txMin;
        //Log.d(TAG, "trying to set arrays");
    }

    public static void setSwipeProgress (int index) {
        swipeIndex = index;
    }

    public static void setThresholds(int txThreshold, int rxThreshold) {

        mTxThreshold = txThreshold;
        mRxThreshold = rxThreshold;

    }

    public static int getMaxToDraw(int TxThresh, int RxThresh, int[] maxTx, int[] minTx, int[] maxRx, int[] minRx) {
        int maxToDraw = TxThresh;
        if (RxThresh > maxToDraw) {
            maxToDraw = RxThresh;
        }

        for (int i=0; i < maxTx.length; i++){
            //Log.d (TAG, "array[" + i + "] = " + maxPlus[i]);
            if (maxTx[i] > maxToDraw) {
                maxToDraw = maxTx[i];
            }
        }
        for (int i=0; i < minTx.length; i++){
            //Log.d (TAG, "array[" + i + "] = " + maxPlus[i]);
            if (minTx[i] > maxToDraw) {
                maxToDraw = minTx[i];
            }
        }
        for (int i=0; i < maxRx.length; i++){
            //Log.d (TAG, "array[" + i + "] = " + maxPlus[i]);
            if (maxRx[i] > maxToDraw) {
                maxToDraw = maxRx[i];
            }
        }
        for (int i=0; i < minRx.length; i++){
            //Log.d (TAG, "array[" + i + "] = " + maxPlus[i]);
            if (minRx[i] > maxToDraw) {
                maxToDraw = minRx[i];
            }
        }
        //Log.d(TAG, "max to draw " + maxToDraw);
        return maxToDraw;

    }

}
