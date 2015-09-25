package com.motorola.ghostbusters;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.concurrent.Semaphore;


public class DisplayTest extends Activity {

    ViewFlipper mFlipper;
    private final static String TAG = "Ghostbusters";
    public static SharedPreferences userPref;
    public static int CYCLES;
    int screenWidth;
    public static String imgNamesToShow[];
    public int imCounter;
    Bitmap mBitmap;
    private static int mCheck;
    public static Semaphore imReadySemaphore;
    static Handler mHandler;
    public static CollectDataForImages mThreadForData;
    public static String[][] standardImgMap;
    public static int testCounter;
    PowerManager.WakeLock mWakeLock;
    Instrumentation inst;
    private volatile static boolean isStarted;
    private volatile static boolean stopTest;
    View decorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        */
        setContentView(R.layout.activity_display_test);
        mFlipper = (ViewFlipper) findViewById(R.id.mDisplay);
        imReadySemaphore = new Semaphore(0);
        testCounter = 0;
        isStarted = false;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        //inst = new Instrumentation();

        mHandler = new Handler() {
            @Override
            public void handleMessage (Message msg) {
                //super.handleMessage(msg);
                //TODO
                if (msg.what < CYCLES) {

                    imCounter = msg.what;
                    Log.d(TAG, "image counter = " + imCounter);
                    if (mCheck != imCounter || imCounter == 0) {
                        mFlipper.removeAllViews();

                        if (mBitmap != null) {
                            mBitmap.recycle();
                        }
                        mBitmap = null;
                        //System.gc();

                        testCounter++;

                        WindowManager.LayoutParams params = getWindow().getAttributes();
                        int res5 = testCounter % 5;
                        int res2 = (testCounter/5)%2;

                        if ((res5 == 0) && (res2 == 1)) {
                            params.screenBrightness = 0;
                            Log.d(TAG, "Setting brightness to 0");
                            getWindow().setAttributes(params);

                            /*
                            try {
                                Process p = Runtime.getRuntime().exec("input keyevent 26");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            */
                        }
                        if ((res5 == 0) && (res2 == 0)) {
                            params.screenBrightness = -1;
                            Log.d(TAG, "Setting brightness back to normal");
                            getWindow().setAttributes(params);
                            /*
                            try {
                                Process p = Runtime.getRuntime().exec("input keyevent 26");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            */
                        }

                        if (imCounter < standardImgMap.length) {
                            setFlipperImages(imCounter);

                            Log.d(TAG, "set image in map " + imCounter + ": " + standardImgMap[imCounter][0]);
                        }

                        Log.d(TAG, "image ready, setting it on display");
                        mFlipper.setDisplayedChild(0);

                        mCheck = imCounter;
                        imReadySemaphore.release();
                        Log.d(TAG, "semaphore is released after image " + imCounter);
                    }
                }
            }
        };

        userPref = PreferenceManager
                .getDefaultSharedPreferences(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        mWakeLock.acquire();
        decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        decorView.setSystemUiVisibility(uiOptions);

        mCheck = 0;
        stopTest = false;
        mFlipper.removeAllViews();
        //imgNames = getImgNames();

        CYCLES = MainActivity.standardEntries;

        Log.d(TAG, "images number = " + CYCLES);

            standardImgMap = new String[MainActivity.standardEntries][3];
            standardImgMap = MainActivity.standardImgMap;

        imgNamesToShow = new String[CYCLES];

        startTest();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_slide_show, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startTest() {
        //blockTouch();
        mThreadForData = new CollectDataForImages(CYCLES);
        if (!isStarted) {
            mThreadForData.start();
            isStarted = true;
        }
    }

        private void setFlipperImages(int i) {
        ImageView mImage = new ImageView(getApplicationContext());
        BitmapFactory.Options mOpts = new BitmapFactory.Options();
        mOpts.inScaled = false;
        mOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        int drawableId;
        mBitmap = null;

            drawableId = getResources().getIdentifier(standardImgMap[i][0], "raw", "com.motorola.ghostbusters");
            //Log.d(TAG, "img " + imgNames[i]);
            imgNamesToShow[i] = standardImgMap[i][0];
            mBitmap = BitmapFactory.decodeResource(getResources(), drawableId, mOpts);
            //Log.d(TAG, "bitmap is processed");
            mImage.setImageBitmap(mBitmap);

        mFlipper.addView(mImage);
        //Log.d(TAG, "images in flipper: " + mFlipper.getChildCount());
    }

    /*
    private void blockTouch() {
        TouchDevice.diagDisableTouch();
        TouchDevice.diagGearAuto(0);
    }

    private void unBlockTouch() {

        TouchDevice.diagGearAuto(0);
        TouchDevice.diagEnableTouch();
    }
    */

    public void onBackPressed() {
        super.onBackPressed();
        stopTest = true;
    }

    public void onPause() {
        super.onPause();
        if (stopTest) {
            //stopTest = true;
            mWakeLock.release();
        }
        Log.d(TAG, "calling on Pause from display test");
        //unBlockTouch();
    }

    //////////////
    public class CollectDataForImages extends Thread {

        private int cycles;

        CollectDataForImages(int cycles) {
            this.cycles = cycles;
        }
        ///
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            //
            //while (!isInterrupted()) {
            Message msg;
            imCounter = 0;
            while (!stopTest) {
            for (int i = 0; i < cycles; i++) {
                if (!stopTest) {

                    try {
                        msg = mHandler.obtainMessage(i);
                        mHandler.sendMessage(msg);
                        Log.d(TAG, "sent request to change image");
                        imReadySemaphore.acquire();
                        long time1 = SystemClock.elapsedRealtime();

                        if (i % 5 == 0) {
                            //inst.sendKeyDownUpSync(KeyEvent.KEYCODE_POWER);
                            /*
                            try {
                                Process p = Runtime.getRuntime().exec("input keyevent 26");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            */
                        }

                        for (int j = 0; j < 300; j++) {
                            try {
                                Thread.sleep(10);
                                //Log.d(TAG, "sleeping 0.05 s...");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        long timeDelta = SystemClock.elapsedRealtime() - time1;
                        Log.d(TAG, "time delta: " + timeDelta + " ms");
                    } catch (InterruptedException e) {
                        //Log.d(TAG, "interrupt");
                        e.printStackTrace();
                    }

                }
            }
        }
        }

    }
///////////////////

}
