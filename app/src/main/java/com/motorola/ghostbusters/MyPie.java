package com.motorola.ghostbusters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.util.Random;

/**
 * Created by elenalast on 8/2/15.
 */
public class MyPie extends View {
    public static final String TAG = "Ghostbusters";
    Paint paintAxis, paintWhite;
    Paint paintThresholds;
    Paint paintLimits, paintHyst;
    Paint paintText;

    float axisX1, axisX2, axisX_Y;
    float axisY_X, axisY1, axisY2;
    float xC, yC, radius1, radius2, radius3;
    public int axisPad = 20;
    public int mTextSize;
    //public int gearsNum;
    float xSize;
    float xStep;
    float paintWidth;
    float startAngle;
    float sweepAngle;
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
    Random rand = new Random();

    public MyPie(Context context) {
        super(context);
        initMyView();
    }

    public MyPie(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMyView();
    }

    public MyPie(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initMyView();
    }

    public void initMyView() {

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = ((Activity) getContext()).getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;

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

        xC = screenWidth/2 - 30;
        yC = screenHeight/4;
        radius3 = screenWidth/7;
        radius2 = screenWidth/5;
        radius1 = screenWidth/3;

        paintAxis = new Paint();
        paintAxis.setColor(Color.MAGENTA);
        paintAxis.setStrokeWidth(3);
        paintAxis.setStyle(Paint.Style.FILL_AND_STROKE);

        paintThresholds = new Paint();
        paintThresholds.setColor(Color.GREEN);
        paintThresholds.setStrokeWidth(5);
        paintThresholds.setStyle(Paint.Style.FILL_AND_STROKE);

        paintLimits = new Paint();
        paintLimits.setColor(Color.GRAY);
        paintLimits.setStrokeWidth(5);
        paintLimits.setStyle(Paint.Style.FILL_AND_STROKE);

        paintHyst = new Paint();
        paintHyst.setColor(Color.parseColor("#FF8300"));
        paintHyst.setStrokeWidth(5);
        paintHyst.setStyle(Paint.Style.FILL_AND_STROKE);

        paintWhite = new Paint();
        paintWhite.setColor(Color.WHITE);
        paintWhite.setStrokeWidth(5);
        paintWhite.setStyle(Paint.Style.FILL_AND_STROKE);

        paintText = new Paint();
        paintText.setColor(Color.RED);
        paintText.setTextSize(mTextSize);

        paintText.setStrokeWidth(2);
        paintText.setStyle(Paint.Style.FILL_AND_STROKE);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        String stats[] = MainActivity.mDevice.diagStats();
        String value1 = "";
        String value2 = "";
        //value += "Statistics: \n";
        for(int i=0; i<stats.length; i++) {
            if (stats[i].startsWith("H")) {
                value2 += stats[i];
            } else {
                value1 += stats[i];
            }
            //Log.d(TAG, "add stat: " + stats[i]);
        }

            RectF oval1 = new RectF((xC - radius1),
                    (yC - radius1), (xC + radius1),
                    (yC + radius1));

            RectF oval2 = new RectF((xC - radius2),
                    (yC - radius2), (xC + radius2),
                    (yC + radius2));

            RectF oval3 = new RectF((xC - radius3),
                    (yC - radius3), (xC + radius3),
                    (yC + radius3));

        if (!value1.isEmpty()) {

            startAngle = 0;
            sweepAngle = 110;
            canvas.drawArc(oval1, startAngle, sweepAngle, true,
                    paintHyst);

            startAngle += sweepAngle;
            sweepAngle = 360 - startAngle;
            canvas.drawArc(oval1, startAngle, sweepAngle, true,
                    paintThresholds);

            canvas.drawCircle(xC, yC, radius2, paintWhite);


            startAngle = 0;
            sweepAngle = 160;
            canvas.drawArc(oval2, startAngle, sweepAngle, true,
                    paintAxis);
            startAngle += sweepAngle;
            sweepAngle = 360 - startAngle;
            canvas.drawArc(oval2, startAngle, sweepAngle, true,
                    paintLimits);

            canvas.drawCircle(xC, yC, radius3, paintWhite);

            startAngle = 0;
            sweepAngle = 75;
            canvas.drawArc(oval3, startAngle, sweepAngle, true,
                    paintHyst);
            startAngle += sweepAngle;
            sweepAngle = 360 - startAngle;
            canvas.drawArc(oval3, startAngle, sweepAngle, true,
                    paintText);
        } else {
            Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ghostbusters_logo);
            canvas.drawBitmap(mBitmap, null, oval1, null);
        }

    }

}

