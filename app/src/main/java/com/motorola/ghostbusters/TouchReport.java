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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by elenalast on 6/9/15.
 */
@SuppressLint({ "DrawAllocation", "ClickableViewAccessibility" })
public class TouchReport extends View {

    public static final String TAG = "Ghostbusters";
    Paint paintRed, paintYellow, paintBlue, paintOrange, paint;

    private int axisPad;
    //private final float xSize;
    float xStep, yStep, radius;
    public static int xDim = 1;
    public static int yDim = 1;
    public static short[] values;
    public static short[][] values2;
    int screenWidth;
    int screenHeight;
    int realScreenHeight;
    float threshold;
    public static boolean isVisible;
    static boolean tmp = false;

    public TouchReport(Context context) {
        super(context);
        initMyView();
    }

    public TouchReport(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMyView();
    }

    public TouchReport(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initMyView();
    }

    public void initMyView() {
        threshold = MainActivity.threshold;
        TouchRpt.getArray();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = ((Activity) getContext()).getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;
        Log.d(TAG, "screen: " + screenWidth +"x"+screenHeight);
        Log.d(TAG, "threshold: " + threshold);
        if (screenHeight < 1280) {
            realScreenHeight = 1280;
            axisPad = 25;
            radius = 10;
        } else if (screenHeight < 1920) {
            realScreenHeight = 1920;
            axisPad = 35;
            radius = 15;
        } else {
            realScreenHeight = 2560;
            axisPad = 50;
            radius = 20;
        }

        paintBlue = new Paint();
        paintBlue.setColor(Color.BLUE);
        paintBlue.setStrokeWidth(2);
        paintBlue.setTextSize(18);
        paintBlue.setStyle(Paint.Style.FILL_AND_STROKE);

        paintYellow = new Paint();
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(2);
        paintYellow.setTextSize(18);
        paintYellow.setStyle(Paint.Style.FILL_AND_STROKE);

        paintOrange = new Paint();
        paintOrange.setColor(Color.parseColor("#FF8300"));
        paintOrange.setStrokeWidth(2);
        paintOrange.setTextSize(18);
        paintOrange.setStyle(Paint.Style.FILL_AND_STROKE);

        paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(2);
        paintRed.setTextSize(18);
        paintRed.setStyle(Paint.Style.FILL_AND_STROKE);

        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        TouchRpt.getArray();

        xStep = screenWidth / yDim;
        yStep = realScreenHeight / xDim;

        for (int j=0; j<yDim; j++) {
            for (int i=0; i<xDim; i++) {

                if (values[i + j* xDim] < 0.5 * threshold) {
                    paint = paintBlue;
                } else if (values[i + j* xDim] < 0.75 * threshold) {
                    paint = paintYellow;
                } else if (values[i + j* xDim] < threshold) {
                    paint = paintOrange;
                } else {
                    paint = paintRed;
                }
                canvas.drawCircle(axisPad + xStep * j, axisPad + yStep * i, radius, paint);
            }
            }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        invalidate();
    }

    //public static void setValues(int i[][]) {
    public static void setValues(short i[]) {
        values = i;
    }

    public static void setDimensions (int xD, int yD) {
            xDim = xD;
            yDim = yD;
    }

    public static void setBtnVisibility(boolean isVis) {
        isVisible = isVis;
    }

    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        switch (action) {
            case (MotionEvent.ACTION_MOVE) :
                TouchRpt.setButtons(false);
                invalidate();
                return true;
            case (MotionEvent.ACTION_UP) :
                tmp = !tmp;
                TouchRpt.setButtons(tmp);
                invalidate();
                return true;
            default :
                return true;
        }
    }

}
