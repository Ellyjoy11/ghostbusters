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
 * Created by elenalast on 6/1/15.
 */
@SuppressLint({ "DrawAllocation", "ClickableViewAccessibility" })
public class ProgressLine extends View {

    public static final String TAG = "Ghostbusters";
    Paint paint1, paint2;

    float X1, X2, Y;
    float step;
    public static int mProgress = 0;
    public static int cycles = 1;
    int screenWidth;
    int screenHeight;

    public ProgressLine(Context context) {
        super(context);
        initMyView();
    }

    public ProgressLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMyView();
    }

    public ProgressLine(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initMyView();
    }

    public void initMyView() {

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = ((Activity) getContext()).getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        //Log.d(TAG, "screen Width: " + screenWidth);
        screenHeight = displaymetrics.heightPixels;

        X1 = 25;
        Y = screenHeight;
        X2 = screenWidth - 25;
        //mProgress = SlideShow.lineToDraw;

        paint1 = new Paint();
        paint1.setColor(Color.BLUE);
        paint1.setStrokeWidth(5);
        paint1.setStyle(Paint.Style.STROKE);

        paint2 = new Paint();
        paint2.setColor(Color.GRAY);
        paint2.setStrokeWidth(5);
        paint2.setStyle(Paint.Style.STROKE);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        step = (screenWidth - 50) / cycles;
        //Log.d(TAG, "cycles " + cycles);
        canvas.drawLine(X1, Y, X1 + mProgress * step, Y, paint1);
        canvas.drawLine(X1 + mProgress * step, Y, X2, Y, paint2);

        invalidate();
    }

    public static void setProgress(int i) {
        mProgress = i;
    }
    public static void setCycles(int i) {
        cycles = i;
    }

}
