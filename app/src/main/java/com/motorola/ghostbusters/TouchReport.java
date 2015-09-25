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
    static boolean needToSwap;

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
        needToSwap = MainActivity.mDevice.diagDeltaFrameFlipXY();
        TouchRpt.getArray();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = ((Activity) getContext()).getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;
        Log.d(TAG, "screen: " + screenWidth +"x"+screenHeight);
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
        //xStep = screenWidth / (yDim - 1);
        //yStep = realScreenHeight / (xDim - 1);

        if (yDim > xDim) {
            xStep = screenWidth / (xDim);
            yStep = realScreenHeight / (yDim);

            for (int j=0; j<xDim; j++) {
                for (int i = 0; i < yDim; i++) {

                    if (values[j + i * xDim] < 0.5 * threshold) {
                        paint = paintBlue;
                    } else if (values[j + i * xDim] < 0.75 * threshold) {
                        paint = paintYellow;
                    } else if (values[j + i * xDim] < threshold) {
                        paint = paintOrange;
                    } else {
                        paint = paintRed;
                    }
                    canvas.drawCircle(axisPad + xStep * j, axisPad + yStep * i, radius, paint);
                }
            }
        } else if (needToSwap && yDim < xDim) {

            //////////
            //1st//
            int tt = 0;
            short[][] values3 = new short[yDim][xDim];
                for (int i=0; i<yDim;i++) {
                    for (int j=0; j<xDim;j++){
                        values3[i][j] = values[tt];
                        tt++;
                    }
                }

            //2nd
            short[][] values2 = new short[yDim*2][xDim/2];
            tt=0;
            for (int i=0;i<yDim*2;i+=2) {
                for (int j=0; j < xDim/2; j++) {
                    values2[i][j]=values3[tt][j];
                }
                tt++;
            }
            tt=0;
            for (int i=1;i<yDim*2;i+=2) {
                for (int j=0; j < xDim/2; j++) {
                    values2[i][j]=values3[tt][j+xDim/2];
                }
                tt++;
            }

            //////////
            //3rd

            short[][] values4 = new short[yDim*2][xDim/2];
            tt=0;
            for (int i=0;i<yDim*2;i+=2) {
                for (int j=0; j < xDim/2; j++) {
                    values4[i][j]=values2[tt][j];
                }
                tt++;
            }

            for (int i=1;i<yDim*2;i+=2) {
                for (int j=0; j < xDim/2; j++) {
                    values4[i][j]=values2[tt][j];
                }
                tt++;
            }

            /////////////////

            //draw points
            xStep = screenWidth / (xDim/2);
            yStep = realScreenHeight / (yDim*2);

            for (int j=0; j<xDim/2; j++) {
                for (int i = 0; i < yDim*2; i++) {

                    if (values4[i][j] < 0.5 * threshold) {
                        paint = paintBlue;
                    } else if (values4[i][j] < 0.75 * threshold) {
                        paint = paintYellow;
                    } else if (values4[i][j] < threshold) {
                        paint = paintOrange;
                    } else {
                        paint = paintRed;
                    }
                    canvas.drawCircle(axisPad + xStep * j, axisPad + yStep * i, radius, paint);
                }
            }
        } else {
            xStep = screenWidth / (yDim);
            yStep = realScreenHeight / (xDim);

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

    public static void setDimensions (int xD, int yD, boolean isSLOC) {
            xDim = xD;
            yDim = yD;
            needToSwap = isSLOC;
        //Log.d(TAG, "display is sloc? " + needToSwap);
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
