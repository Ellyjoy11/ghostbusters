package com.motorola.ghostbusters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.util.Random;

import static android.graphics.Color.*;

/**
 * Created by elenalast on 8/2/15.
 */
public class MyPie extends View {
    public static final String TAG = "Ghostbusters";
    Paint paintAxis, paintWhite;
    Paint paintThresholds;
    Paint paintLimits, paint;
    Paint paintText;

    float axisX1, axisX2, axisX_Y;
    float axisY_X, axisY1, axisY2;
    float xC, yC, radius1, radius2, radius3, xLegend, yLegend;
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
    public static int[] gearsStats;
    public static int[] nStats;
    public static int[] mStats;
    int[] mColors;

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
            mTextSize = 40;
            realScreenHeight = 1920;
        } else {
            //axisPad = 50;
            mTextSize = 50;
            realScreenHeight = 2560;
        }

        gearsStats = new int[MainActivity.mDevice.diagGearCount()];
        Log.d(TAG, "length of array is " + gearsStats.length);
        nStats = new int[2];
        mStats = new int[2];

        radius3 = screenWidth/8.5f;
        radius2 = screenWidth/4.25f;
        radius1 = screenWidth/2.75f;

        xC = radius1 + 30;
        yC = screenHeight/4;

        mColors = new int[]{
                Color.GRAY, Color.GREEN, Color.RED, Color.YELLOW,
                Color.BLUE, Color.CYAN, Color.MAGENTA, Color.DKGRAY,
                Color.parseColor("#ff7373"), Color.parseColor("#088da5"), Color.parseColor("#ffc0cb"), Color.parseColor("#800080"),
                Color.parseColor("#00ff7f"), Color.parseColor("#c39797"), Color.parseColor("#31698a"), Color.parseColor("#ffff66")
        };

        paintAxis = new Paint();
        paintAxis.setColor(MAGENTA);
        paintAxis.setStrokeWidth(3);
        paintAxis.setStyle(Paint.Style.FILL_AND_STROKE);

        paintThresholds = new Paint();
        paintThresholds.setColor(GREEN);
        paintThresholds.setStrokeWidth(5);
        paintThresholds.setStyle(Paint.Style.FILL_AND_STROKE);

        paintLimits = new Paint();
        paintLimits.setColor(GRAY);
        paintLimits.setStrokeWidth(5);
        paintLimits.setStyle(Paint.Style.FILL_AND_STROKE);

        paint = new Paint();
        paint.setColor(parseColor("#FF8300"));
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        paintWhite = new Paint();
        paintWhite.setColor(WHITE);
        paintWhite.setStrokeWidth(5);
        paintWhite.setStyle(Paint.Style.FILL_AND_STROKE);

        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(mTextSize);

        paintText.setStrokeWidth(2);
        paintText.setStyle(Paint.Style.FILL_AND_STROKE);

    }

    public static void arrangeArrays(int gearsCount) {

    }

    @Override
    protected void onDraw(Canvas canvas) {

        String stats[] = MainActivity.mDevice.diagStats();
        String value1 = "";
        String value2 = "";
        //String tttt = "G1: 56%";
        String pattern1 = ".*:\\s+(\\d+)\\s*.*\\n";
        int countG = 0;
        int countN = 0;
        int countM = 0;
        //value += "Statistics: \n";
        for(int i=0; i<stats.length; i++) {
            if (stats[i].startsWith("H")) {
                value2 += stats[i];
            } else {
                value1 += stats[i];
                //Log.d(TAG, "add stat: " + stats[i]);
                if (stats[i].matches(pattern1)) {
                    //Log.d(TAG, "string from real statistics matches pattern: '" + stats[i] + "'");
                    String tmp = stats[i].replaceAll(pattern1, "$1");
                    //Log.d(TAG, "tmp: " + tmp);
                    if (stats[i].startsWith("G")) {
                        gearsStats[countG] = Integer.parseInt(tmp);
                        Log.d(TAG, "gearsStats written for gear" + countG +": " + gearsStats[countG]);
                        countG++;
                    } else if (stats[i].startsWith("N")) {
                        nStats[countN] = Integer.parseInt(tmp);
                        Log.d(TAG, "nStats written for N" + countN +": " + nStats[countN]);
                        countN++;
                    } else if (stats[i].startsWith("M")) {
                        mStats[countM] = Integer.parseInt(tmp);
                        Log.d(TAG, "mStats written for M" + countM +": " + mStats[countM]);
                        countM++;
                    }
                } else {
                    Log.d(TAG, "string from real statistics '" + stats[i] + "' checked - no match");
                }
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

            xLegend = xC + radius1 + 30;
            yLegend = yC - radius1 + 30;
            startAngle = 0;
            canvas.drawCircle(xC, yC, radius1, paintWhite);
            for (int i=0; i < MainActivity.mDevice.diagGearCount(); i++) {
                sweepAngle = 360 * gearsStats[i] / 100;
                paint.setColor(mColors[i]);
                if (sweepAngle != 0) {
                    canvas.drawArc(oval1, startAngle, sweepAngle, true,
                            paint);
                }
                canvas.drawRect(xLegend, yLegend, xLegend + 30, yLegend + 30, paint);
                yLegend += mTextSize;
                String textForLegend = "G" + i;
                canvas.drawText(textForLegend, xLegend + mTextSize, yLegend - 15, paintText);
                startAngle += sweepAngle;
            }

            //canvas.drawCircle(xC, yC, radius2, paintWhite);

            //Drawing Metal Plate Detection stats in med circle
            canvas.drawCircle(xC, yC, radius2, paintWhite);

            startAngle = 0;
            sweepAngle = 360 * mStats[0] / 100;
            paint.setColor(mColors[8]);
            canvas.drawArc(oval2, startAngle, sweepAngle, true,
                    paint);
            canvas.drawRect(xLegend, yLegend, xLegend + 30, yLegend + 30, paint);
            yLegend += mTextSize;
            canvas.drawText("M0", xLegend + mTextSize, yLegend - 15, paintText);
            startAngle += sweepAngle;
            sweepAngle = 360 * mStats[1] / 100;
            //Log.d(TAG, "sweep " + sweepAngle);
            paint.setColor(mColors[9]);
            if (sweepAngle != 0) {
                canvas.drawArc(oval2, startAngle, sweepAngle, true,
                        paint);
            }
            canvas.drawRect(xLegend, yLegend, xLegend+30, yLegend+30, paint);
            yLegend += mTextSize;
            canvas.drawText("M1", xLegend + mTextSize, yLegend - 15, paintText);

            //canvas.drawCircle(xC, yC, radius3, paintWhite);
            //Drawing Noise Mitigation stats in inner circle
            canvas.drawCircle(xC, yC, radius3, paintWhite);
            startAngle = 0;
            sweepAngle = 360 * nStats[0] / 100;
            paint.setColor(mColors[10]);
            canvas.drawArc(oval3, startAngle, sweepAngle, true,
                    paint);
            canvas.drawRect(xLegend, yLegend, xLegend + 30, yLegend + 30, paint);
            yLegend += mTextSize;
            canvas.drawText("HNM", xLegend + mTextSize, yLegend - 15, paintText);
            startAngle += sweepAngle;
            sweepAngle = 360 * nStats[1] / 100;
            paint.setColor(mColors[11]);
            if (sweepAngle != 0) {
                canvas.drawArc(oval3, startAngle, sweepAngle, true,
                        paint);
            }
            canvas.drawRect(xLegend, yLegend, xLegend+30, yLegend+30, paint);
            yLegend += mTextSize;
            canvas.drawText("FNM", xLegend + mTextSize, yLegend - 15, paintText);

        }

    }

}

