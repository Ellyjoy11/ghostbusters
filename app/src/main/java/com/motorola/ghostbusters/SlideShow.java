package com.motorola.ghostbusters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;


public class SlideShow extends Activity {
    ViewFlipper mFlipper;
    private final static String TAG = "Ghostbusters";
    ArrayList<String> mFiles = new ArrayList<String>();
    ArrayList<Integer> stat = new ArrayList<Integer>();
    private boolean isDone = false;

    ProgressLine myView;
    public static int lineToDraw;
    public static SharedPreferences userPref;
    public static boolean addCustom = false;
    public static String customPath = "";
    public static int CYCLES;
    public static int ACTUAL_NUM;// = 25;
    public static ArrayList<String> imgNames;
    int screenWidth;
    public static int samples;
    public static int gearsSet;
    public static int samplesDone;
    public static int gearsSetDone=0;
    public static int gearsSetDoneRx=0;
    public static String testTypeDone="";

    public static String imgUri;
    public static String imgUriDone;

    public static int mRxMax[][];
    public static int mRxMin[][];
    public static int mTxMax[][];
    public static int mTxMin[][];
    public static int mMaxIm[][];
    public static int mMinIm[][];

    public static int cycleTestCounter;

    public static String imgNamesToShow[];

    public int imCounter;
    Bitmap mBitmap;
    private static int mCheck;
    public static int tmpCount;
    public static Semaphore imReadySemaphore;
    static Handler mHandler;
    public static final int FINISH = 500;
    public static CollectDataForImages mThreadForData;
    public volatile boolean isStopped;
    public static String imgMaskAsHex = "";
    public static String maskAsBinaryString;
    public static String[][] standardImgMap;
    public static int c;
    public static boolean isDefault;
    public static int maxmin;
    public static int maxminRx;
    public static int [] maxminRxTx;
    public static boolean needSleep;
    public static boolean isRxEnabled;
    public static boolean isTxEnabled;

    public static int intTime2[];
    public static int intTimeBase2;
    public static int intTime59[];
    public static int intTimeBase59;
    public static int intTimeRange;
    public static int TEST_CYCLES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_slide_show);
        mFlipper = (ViewFlipper) findViewById(R.id.mFlipper);
        myView = (ProgressLine) findViewById(R.id.progressLine);

        userPref = PreferenceManager
                .getDefaultSharedPreferences(this);
        //cycleTestCounter = userPref.getInt("cycle_counter", 0);

        imReadySemaphore = new Semaphore(0);

        mHandler = new Handler() {
            @Override
        public void handleMessage (Message msg) {
                //super.handleMessage(msg);
                //TODO
                //Log.d(TAG, "Handler's got message to process");
                if (msg.what < CYCLES) {

                    imCounter = msg.what;
                    //Log.d(TAG, "image counter = " + imCounter);
                    if (mCheck != imCounter || imCounter == 0) {
                        mFlipper.removeAllViews();

                        if (mBitmap != null) {
                            mBitmap.recycle();
                        }
                        mBitmap = null;
                        //System.gc();

                        if (imCounter == (CYCLES - 1) && MainActivity.currentUri != null) {
                            addSingleImage(MainActivity.currentUri);
                        } else if (imCounter >= standardImgMap.length && imCounter < CYCLES && addCustom) {
                            setFlipperImages(imCounter - standardImgMap.length, true);
                        }

                        if (imCounter < standardImgMap.length) {
                            setFlipperImages(imCounter, false);
                            //Log.d(TAG, "set image in map " + imCounter + ": " + standardImgMap[imCounter][0]);
                        }

                        //Log.d(TAG, "image ready, setting it on display");
                        mFlipper.setDisplayedChild(0);

                        mCheck = imCounter;
                        imReadySemaphore.release();
                        tmpCount++;
                        //Log.d(TAG, "semaphore is released after image " + imCounter);
                    }
                }

                else if (msg.what > 999) {

                    lineToDraw = tmpCount * (MainActivity.gearsCount+1) + (msg.what - 1000);
                    //Log.d(TAG, "progress for line is " + lineToDraw + "/" + CYCLES);
                    myView.setProgress(lineToDraw + 1);
                    myView.invalidate();
                    imReadySemaphore.release();
                    //Log.d(TAG, "semaphore is released after progress update " + lineToDraw + " / " + ACTUAL_NUM * (MainActivity.gearsCount+1));
                }

                 else if (msg.what == FINISH) {

                    unBlockTouch();
                    if (MainActivity.testType.contains("2")) {
                        TouchDevice.diagSetTranscapIntDur(userPref.getInt("startIntDur2", intTimeBase2));
                        Log.d(TAG, "setting int time back to: " + userPref.getInt("startIntDur2", intTimeBase2));
                        TouchDevice.diagForceUpdate();
                    } else if (MainActivity.testType.contains("59")) {
                        TouchDevice.diagSetHybridIntDur(userPref.getInt("startIntDur59", intTimeBase59));
                        Log.d(TAG, "setting int time back to: " + userPref.getInt("startIntDur59", intTimeBase59));
                        TouchDevice.diagSetHybridStretchDur(MainActivity.baseStretch);
                        TouchDevice.diagForceUpdate();
                    }
                    isDone = true;

                    mFlipper.removeAllViews();
                    if (mBitmap != null) {
                        mBitmap.recycle();
                    }
                    mBitmap = null;
                    Log.d(TAG, "test counter="+cycleTestCounter);
                        if (cycleTestCounter < MainActivity.TEST_CYCLES-1) {
                            gearsSetDone = 0;
                            gearsSetDoneRx = 0;
                            cycleTestCounter++;
                            SharedPreferences.Editor editor = userPref.edit();
                            editor.putInt("cycle_counter", cycleTestCounter);
                            editor.commit();
                            Log.d(TAG, "increase counter: " + cycleTestCounter);
                            finish();
                            startActivity(getIntent());
                        } else {
                            samplesDone = samples;
                            testTypeDone = MainActivity.testType;
                            Log.d(TAG, "record: " + testTypeDone);
                            if (testTypeDone.contains("2")) {
                                gearsSetDone = gearsSet;
                            } else if (testTypeDone.contains("59")) {
                                gearsSetDoneRx = gearsSet;
                            }
                            Log.d(TAG, "gearsSet done: " + gearsSetDone + "gearsSetRx done: " + gearsSetDoneRx);
                            imgUriDone = imgUri;
                            showResults();
                        }
                    }
                }
        };

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = this.getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        Log.d(TAG, "screen width is " + screenWidth);

    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.d(TAG, "call onResume");
        isDefault = false;
        c = 0;
        tmpCount = -1;

        userPref = PreferenceManager
                .getDefaultSharedPreferences(this);

        cycleTestCounter = userPref.getInt("cycle_counter", 0);

        if (cycleTestCounter == 0) {
            intTimeBase2 = TouchDevice.diagTranscapIntDur();
            intTimeBase59 = TouchDevice.diagHybridIntDur();
            SharedPreferences.Editor editor = userPref.edit();
            editor.putInt("startIntDur2", intTimeBase2);
            editor.putInt("startIntDur59", intTimeBase59);
            editor.commit();
        }

        intTimeRange = Integer.parseInt(userPref.getString("int_time", "0"));
        TEST_CYCLES = MainActivity.TEST_CYCLES;

        intTime2 = new int[TEST_CYCLES];
        intTime59 = new int[TEST_CYCLES];

        for (int j=0; j < TEST_CYCLES; j++) {
            intTime2[j] = userPref.getInt("startIntDur2", intTimeBase2) - intTimeRange + j;
            //Log.d(TAG, "set to array: " + intTime2[j]);
            intTime59[j] = userPref.getInt("startIntDur59", intTimeBase59) - intTimeRange + j;
        }
        MainActivity.intTime2 = intTime2;
        MainActivity.intTime59 = intTime59;

        if (MainActivity.testType.contains("2")) {
            TouchDevice.diagSetTranscapIntDur(intTime2[cycleTestCounter]);
            TouchDevice.diagForceUpdate();
            Log.d(TAG, "called force update after set intTranscapDur");
        } else if (MainActivity.testType.contains("59")) {
            TouchDevice.diagSetHybridIntDur(intTime59[cycleTestCounter]);
            TouchDevice.diagForceUpdate();
            Log.d(TAG, "called force update after set intDur");
        }

        if (MainActivity.testType.contains("2") && TEST_CYCLES != userPref.getInt("cycles_done_2", 0)) {
            gearsSetDone = 0;
        }
        if (MainActivity.testType.contains("59") && (TEST_CYCLES != userPref.getInt("cycles_done_59", 0) ||
                MainActivity.stretches != userPref.getInt("stretches_done", 0))) {
            gearsSetDoneRx = 0;
        }

        isRxEnabled = MainActivity.isRxEnabled;
        isTxEnabled = MainActivity.isTxEnabled;

        isDone = false;
        needSleep = true;
        isStopped = userPref.getBoolean("isStop", false);
        mCheck = 0;
        myView.setBackgroundColor(Color.TRANSPARENT);
        mFiles.clear();
        mFlipper.removeAllViews();

        addCustom = userPref.getBoolean("custom", false);
        //Log.d(TAG, "add custom: " + addCustom);
        samples = Integer.parseInt(userPref.getString("samples", "100"));
        gearsSet = userPref.getInt("gearsMask", 0);
        Log.d(TAG, "read gears set from pref: " + gearsSet);
        //isModeAuto = userPref.getBoolean("isAuto", true);
        imgUri = userPref.getString("uri", null);
        imgNames = getImgNames();


        if (imgNames != null) {
            ACTUAL_NUM = imgNames.size();
        } else {
            ACTUAL_NUM = 0;
        }

            CYCLES = MainActivity.standardEntries;

        //Log.d(TAG, "cycles = " + CYCLES + "; act/num " + ACTUAL_NUM);
        if (addCustom) {
            customPath = userPref.getString("custom_path", "");
            addCustomImages(customPath);
            CYCLES += mFiles.size();
            ACTUAL_NUM += mFiles.size();
            Log.d(TAG, "added " + mFiles.size() + " images from custom folder");
        }
        Log.d(TAG, "image uri: " + MainActivity.currentUri);

        if (MainActivity.currentUri != null) {
            CYCLES += 1;
            ACTUAL_NUM += 1;
        }

        //Log.d(TAG, "isDone and isStopped: " + isDone + " / " + isStopped);

        Log.d(TAG, "compare test type: " + testTypeDone + " and " + MainActivity.testType);
        Log.d(TAG, "compare gears set: " + gearsSetDone + " and " + gearsSet);
        Log.d(TAG, "compare gears set for Rx: " + gearsSetDoneRx + " and " + gearsSet);
        if (standardImgMap == null || mMaxIm == null || CYCLES > mMaxIm.length
                || (imgUri != null && !imgUriDone.equals(imgUri)) || isStopped
                || samplesDone != samples ||
                gearsSetDone != gearsSet || gearsSetDoneRx != gearsSet ||
                mRxMax == null || !testTypeDone.equals(MainActivity.testType)) {
            isStopped = false;
            SharedPreferences.Editor editor = userPref.edit();
            editor.putBoolean("isStop", isStopped);
            editor.commit();
                standardImgMap = new String[MainActivity.standardEntries][3];
                standardImgMap = MainActivity.standardImgMap;
             if (testTypeDone.isEmpty() ||
                     (!testTypeDone.isEmpty() && !testTypeDone.equals(MainActivity.testType) && testTypeDone.contains("59")) ||
                     (testTypeDone.equals(MainActivity.testType) && testTypeDone.contains("2")) || gearsSetDone != gearsSet) {

                 editor.putBoolean("report2_data_exists", false);
                 editor.commit();
                 mMaxIm = new int[CYCLES][TouchDevice.diagGearCount() + 1];
                 mMinIm = new int[CYCLES][TouchDevice.diagGearCount() + 1];
             }

             if (testTypeDone.isEmpty() ||
                     (!testTypeDone.isEmpty() && !testTypeDone.equals(MainActivity.testType) && testTypeDone.contains("2")) ||
                     (testTypeDone.equals(MainActivity.testType) && testTypeDone.contains("59")) || gearsSetDoneRx != gearsSet) {

                 editor.putBoolean("report59_data_exists", false);
                 editor.commit();
                 mRxMax = new int[CYCLES][MainActivity.stretches];
                 mRxMin = new int[CYCLES][MainActivity.stretches];
                 mTxMax = new int[CYCLES][MainActivity.stretches];
                 mTxMin = new int[CYCLES][MainActivity.stretches];
                 maxminRxTx = new int [4];
             }
            for (int i = 0; i < MainActivity.standardEntries; i++) {
                //standardImgMap[i][1] = "0";
                standardImgMap[i][2] = "0";
            }
        } else {
            for (int i = 0; i < MainActivity.standardEntries; i++) {
                //standardImgMap[i][0] = MainActivity.standardImgMap[i][0];
                standardImgMap[i][1] = MainActivity.standardImgMap[i][1];
            }
        }

        if (ACTUAL_NUM == 0) {
            Log.d(TAG, "trying add default");
            addDefaultBlack(screenWidth);
            isDefault = true;
            ACTUAL_NUM += 1;
        }
        Log.d(TAG, "actual number of images for test: " + ACTUAL_NUM);
        imgNamesToShow = new String[CYCLES];

        imgMaskAsHex = MainActivity.imgMaskAsString;

        if (imgNames.size() != 0) {
            Log.d(TAG, "img selected");

        } else if (addCustom) {
            Log.d(TAG, "only custom path selected");

        } else if (MainActivity.currentUri != null) {
            Log.d(TAG, "the only image is single");

        }

        //Log.d(TAG, "init cycles " + CYCLES);
        lineToDraw = c;
        myView.setProgress(lineToDraw);
        myView.setCycles(ACTUAL_NUM * (MainActivity.gearsCount+1));
        myView.invalidate();

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
        blockTouch();
        mThreadForData = new CollectDataForImages(CYCLES);
        mThreadForData.start();
    }

    public static ArrayList<String> getImgNames() {
        int imgCount = 0;
        ArrayList<String> imgs = new ArrayList<String>();
        imgs.clear();
        for (int i=0; i<MainActivity.standardEntries; i++) {
            //Log.d(TAG, "..." + MainActivity.standardImgMap[i][1]);
            if (MainActivity.standardImgMap[i][1].equals("1")) {
                imgCount++;
                imgs.add(MainActivity.standardImgMap[i][0]);
            }
        }

        return imgs;
    }

    public static String getImgMask() {

        maskAsBinaryString = "";
        String imgMaskAsString = "";

        boolean[] checked = new boolean[MainActivity.standardEntries];
        for (int i=0; i < MainActivity.standardEntries; i++) {
            checked[i] = false;
            for (int j=0; j < imgNames.size(); j++) {
                if (MainActivity.valSet[i].equals(imgNames.get(j))) {
                    checked[i] = true;
                    continue;
                }
            }

            standardImgMap[i][0] = MainActivity.valSet[i];

            if (checked[i]) {
                maskAsBinaryString += "1";
                standardImgMap[i][1] = "1";
            } else {
                maskAsBinaryString += "0";
                standardImgMap[i][1] = "0";
            }
        }
        String imgMask = new StringBuilder(maskAsBinaryString).reverse().toString();
        //Log.d(TAG, "string mask for images: " + maskAsBinaryString + "; length is " + maskAsBinaryString.length());
        int decimal = Integer.parseInt(imgMask,2);
        imgMaskAsString = "0x" + Integer.toString(decimal, 16);
        //Log.d(TAG, "try to save mask as hex: " + imgMask + " -> " + imgMaskAsString);
        return imgMaskAsString;
    }

    private void setFlipperImages(int i, boolean isCustom) {
        ImageView mImage = new ImageView(getApplicationContext());
        BitmapFactory.Options mOpts = new BitmapFactory.Options();
        mOpts.inScaled = false;
        mOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        int drawableId;
        mBitmap = null;

        if (isCustom) {
            File imgFile = new File(mFiles.get(i));
            //Log.d(TAG, "img from custom path: " + imgFile.toString());
            imgNamesToShow[i+standardImgMap.length] = imgFile.toString();
            mBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), mOpts);
            mImage.setImageBitmap(mBitmap);

        } else {

            drawableId = getResources().getIdentifier(standardImgMap[i][0], "raw", "com.motorola.ghostbusters");
            //Log.d(TAG, "img " + imgNames[i]);
            imgNamesToShow[i] = standardImgMap[i][0];
            mBitmap = BitmapFactory.decodeResource(getResources(), drawableId, mOpts);
            Log.d(TAG, "bitmap is processed, alloc size is " + mBitmap.getAllocationByteCount());
            //Log.d(TAG, "bitmap is processed, size in bytes is " + mBitmap.getByteCount());
            mImage.setImageBitmap(mBitmap);
        }
        mFlipper.addView(mImage);
        //Log.d(TAG, "images in flipper: " + mFlipper.getChildCount());
    }

    private void addCustomImages(String pathToImages) {
        File mImgs = new File(pathToImages);
        getFilesList(mImgs.getAbsolutePath());
    }


    private void addSingleImage(Uri mUri) {
        ImageView mImage = new ImageView(getApplicationContext());
        BitmapFactory.Options mOpts = new BitmapFactory.Options();
        mOpts.inScaled = false;
        mOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        mBitmap = null;

        try {
            mBitmap = getBitmapFromUri(mUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mImage.setImageBitmap(mBitmap);
        mFlipper.addView(mImage);
        imgNamesToShow[CYCLES-1] = "user_added";
        //Log.d(TAG, "images in flipper with single added: " + mFlipper.getChildCount());
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    private void addDefaultBlack(int resolution) {
        String imName = "";
        imgNames = new ArrayList<String>();
        //Log.d(TAG, "created array of " + imgNames.length);
        if (resolution == 720) {
            imName = "blackscreen_720";
        } else if (resolution == 1080) {
            imName = "blackscreen_1080";
        } else if (resolution == 1440) {
            imName = "blackscreen_1440";
        }
        imgNames.add(imName);
        standardImgMap = new String[MainActivity.standardEntries][3];
        String mask = "";
        standardImgMap[0][0] = imName;
        standardImgMap[0][1] = "1";
        mask += "1";
        standardImgMap[0][2] = "0";
        for (int i = 1; i < MainActivity.standardEntries; i++) {
            standardImgMap[i][1] = "0";
            standardImgMap[i][2] = "0";
            mask += "0";
        }
        SharedPreferences.Editor editor = userPref.edit();
        editor.putString("imgMask", mask);
        editor.commit();
        Log.d(TAG, "no images are selected by user, setting default black");
    }

        public void showResults() {
            if (isDone && !isStopped) {
                Toast.makeText(getApplicationContext(), "Test is done",
                        Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = userPref.edit();
                editor.putInt("cycle_counter", 0);
                if (testTypeDone.contains("2")) {
                    editor.putInt("cycles_done_2", cycleTestCounter+1);
                }
                if (testTypeDone.contains("59")) {
                    editor.putInt("cycles_done_59", cycleTestCounter+1);
                    editor.putInt("stretches_done", MainActivity.stretches);
                }

                editor.commit();
                    Intent intent = new Intent(this, CycleTestChart.class);
                    startActivity(intent);
        } else {
                finish();
                Intent intent = new Intent(getApplicationContext(),
                        MainActivity.class);
                startActivity(intent);
            }
        }

    private void blockTouch() {
        TouchDevice.diagDisableTouch();
        TouchDevice.diagGearAuto(1);
        Log.d(TAG, "called disable touch function");
    }

    private void unBlockTouch() {

        TouchDevice.diagGearAuto(0);
        TouchDevice.diagEnableTouch();
    }

    public void onPause() {
        super.onPause();

        //Log.d(TAG, "call onPause");

        if (!isDone) {
            isStopped = true;
            unBlockTouch();
            if (MainActivity.testType.contains("2")) {
                TouchDevice.diagSetTranscapIntDur(userPref.getInt("startIntDur2", intTimeBase2));
                Log.d(TAG, "setting int time back to: " + userPref.getInt("startIntDur2", intTimeBase2));
                TouchDevice.diagForceUpdate();
            } else if (MainActivity.testType.contains("59")) {
                TouchDevice.diagSetHybridIntDur(userPref.getInt("startIntDur59", intTimeBase59));
                Log.d(TAG, "setting int time back to: " + userPref.getInt("startIntDur59", intTimeBase59));
                TouchDevice.diagSetHybridStretchDur(MainActivity.baseStretch);
                TouchDevice.diagForceUpdate();
            }
            finish();
            Intent intent = new Intent(getApplicationContext(),
                    MainActivity.class);
            startActivity(intent);
        } else {
            isStopped = false;
        }
        SharedPreferences.Editor editor = userPref.edit();
        editor.putBoolean("isStop", isStopped);
        editor.commit();
    }

    private void getFilesList(String mPath) {

        File root = new File(mPath);
        File[] list = root.listFiles();

        if (list != null) {
            for (File f : list) {
                if (f.getAbsolutePath().endsWith("jpg") ||
                        f.getAbsolutePath().endsWith("jpeg") ||
                        f.getAbsolutePath().endsWith("png") ||
                        f.getAbsolutePath().endsWith("bmp") ||
                        f.getAbsolutePath().endsWith("gif")) {
                    mFiles.add(f.getAbsolutePath());
                    //Log.d(TAG, "file " + f.getAbsolutePath());
                }
            }
        }

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

            Message msg;
            imCounter = 0;

            for (int i = 0; i < cycles; i++) {
                if (!isStopped) {

                try {
                    if ((i < standardImgMap.length && standardImgMap[i][1].equals("1") && standardImgMap[i][2].equals("0")) ||
                            i >= standardImgMap.length) {


                        msg = mHandler.obtainMessage(i);
                        mHandler.sendMessage(msg);
                        //Log.d(TAG, "sent request to change image");
                        imReadySemaphore.acquire();

                        if (needSleep){
                            try {
                                Log.d(TAG, "Pause 10 s before running the test");
                                Thread.sleep(10000);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            needSleep = false;
                        }

                        ////////report type 2 test loop
                        if (MainActivity.testType.contains("2")) {
                            for (int gear = 0; gear <= MainActivity.gearsCount; gear++) {

                                msg = mHandler.obtainMessage(gear + 1000);
                                mHandler.sendMessage(msg);
                                //Log.d(TAG, "sent request to change progress");
                                imReadySemaphore.acquire();

                                if (gear == 0) {
                                    //Log.d(TAG, "sleeping 2 s to get image on screen...");
                                    try {
                                        Thread.sleep(2000);

                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                //Log.d(TAG, "start test for gear " + gear);
                                if (gear < MainActivity.gearsCount && String.valueOf(MainActivity.gearsEnabled[gear]).equals("0")) {
                                    continue;
                                } else {
                                    if (gear == MainActivity.gearsCount) {
                                        int tmpMinValue = 0;
                                        int bestGear = 0;
                                        if (mMaxIm != null) {
                                            bestGear = 0;
                                            tmpMinValue = mMaxIm[i][0];
                                            for (int ll = 1; ll < gear; ll++) {
                                                if (String.valueOf(MainActivity.gearsEnabled[ll]).equals("1") &&
                                                        (mMaxIm[i][ll] < tmpMinValue ||
                                                                String.valueOf(MainActivity.gearsEnabled[bestGear]).equals("0"))) {
                                                    tmpMinValue = mMaxIm[i][ll];
                                                    bestGear = ll;
                                                } else {
                                                    continue;
                                                }
                                            }
                                        }

                                        TouchDevice.diagGearSelect(bestGear);
                                        Log.d(TAG, "Switch to best gear before auto test: Gear " + bestGear);
                                        TouchDevice.diagGearAuto(0);
                                    } else {
                                        TouchDevice.diagGearAuto(1);
                                        TouchDevice.diagGearSelect(gear);
                                    }

                                }
                                //Log.d(TAG, "getting data using diagDeltaPeaks for " + samples + " samples");
                                    Log.d(TAG, "test type is " + MainActivity.testType);
                                    maxmin = MainActivity.mDevice.diagDeltaPeaks(samples);
                                    //Log.d(TAG, "getting max and min from data");
                                    mMaxIm[i][gear] = maxmin / 65536;
                                    mMinIm[i][gear] = maxmin % 65536;
                                if (mMaxIm != null) {
                                    // if (MainActivity.isCycleTest) {
                                    MainActivity.mMaxImC[cycleTestCounter][i][gear] = mMaxIm[i][gear];
                                    MainActivity.mMinImC[cycleTestCounter][i][gear] = mMaxIm[i][gear];
                                    int tmpEvCount = TouchDevice.diagTouchEventCount();
                                    MainActivity.eventCountReport2[cycleTestCounter][gear] += tmpEvCount;
                                    // }

                                    Log.d(TAG, "max/min for image " + imgNamesToShow[i] + " for gear " + gear + ": " + mMaxIm[i][gear] + "/" + mMinIm[i][gear]);
                                    Log.d(TAG, "adding event counts: " + tmpEvCount + "; total events for test cycle " + cycleTestCounter + " for gear " + gear + " = " + MainActivity.eventCountReport2[cycleTestCounter][gear]);
                                    TouchDevice.diagDisableTouch();
                                } else {
                                    errorDialog();
                                }
                            }
                        } else if (MainActivity.testType.contains("59")) {
                            ////////////////////////////////////
                            //report 59 test as separate loop
                            for (int stretch = MainActivity.baseStretch; stretch < MainActivity.baseStretch + MainActivity.stretches; stretch++) {

                                msg = mHandler.obtainMessage(stretch + 1000);
                                mHandler.sendMessage(msg);
                                //Log.d(TAG, "sent request to change progress");
                                imReadySemaphore.acquire();

                                if (stretch == 0) {
                                    //Log.d(TAG, "sleeping 2 s to get image on screen...");
                                    try {
                                        Thread.sleep(2000);

                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                TouchDevice.diagSetHybridStretchDur(stretch);
                                TouchDevice.diagForceUpdate();
                                Log.d(TAG, "called force update after set stretch");

                                //Log.d(TAG, "start test for gear " + gear);

                                    Log.d(TAG, "test type is " + MainActivity.testType);
                                    //maxminRx = MainActivity.mDevice.diagRxDeltaPeaks(samples);
                                    maxminRxTx = MainActivity.mDevice.diagRxTxDeltaPeaks(samples, 59);
                                    //Log.d(TAG, "getting max and min from data");
                                if (maxminRxTx != null) {
                                    mRxMax[i][stretch] = maxminRxTx[3];
                                    mRxMin[i][stretch] = maxminRxTx[2];
                                    mTxMax[i][stretch] = maxminRxTx[1];
                                    mTxMin[i][stretch] = maxminRxTx[0];

                                    // if (MainActivity.isCycleTest) {
                                    MainActivity.mMaxRxImC[cycleTestCounter][i][stretch] = mRxMax[i][stretch];
                                    MainActivity.mMinRxImC[cycleTestCounter][i][stretch] = mRxMin[i][stretch];
                                    MainActivity.mMaxTxImC[cycleTestCounter][i][stretch] = mTxMax[i][stretch];
                                    MainActivity.mMinTxImC[cycleTestCounter][i][stretch] = mTxMin[i][stretch];
                                    int tmpEvCount = TouchDevice.diagTouchEventCount();
                                    MainActivity.eventCountReport59[cycleTestCounter][stretch] += tmpEvCount;
                                    // }
                                    Log.d(TAG, "Rx max/min for image " + imgNamesToShow[i] + " for gear " + stretch + ": " + mRxMax[i][stretch] + "/" + mRxMin[i][stretch]);
                                    Log.d(TAG, "Tx max/min for image " + imgNamesToShow[i] + " for gear " + stretch + ": " + mTxMax[i][stretch] + "/" + mTxMin[i][stretch]);
                                    Log.d(TAG, "adding event counts: " + tmpEvCount + "; total events for test cycle " + cycleTestCounter + " for stretch " + stretch + " = " + MainActivity.eventCountReport59[cycleTestCounter][stretch]);
                                    TouchDevice.diagDisableTouch();
                                } else {
                                    errorDialog();
                                }
                            }
                        }
                        ///////////////////////////////////////////
                        if (i < standardImgMap.length) {
                            standardImgMap[i][2] = "1";
                            //Log.d(TAG, "Setting tested value to true for image " + i);
                        }
                    } else if (i < standardImgMap.length && (standardImgMap[i][1].equals("0")
                            && standardImgMap[i][2].equals("0"))) {
                        standardImgMap[i][2] = "0";
                        //Log.d(TAG, "Setting tested value to false for image " + i);
                    }
                } catch (InterruptedException e) {
                    //Log.d(TAG, "interrupt");
                    e.printStackTrace();
                }
                }
        }
                msg = mHandler.obtainMessage(FINISH);
                mHandler.sendMessage(msg);
        }

    }
///////////////////
    public void errorDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
        builder.setMessage(
                "Something went wrong.\n"
                        + "Please try again after HW reset")
                .setTitle("Oops...");

        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //moveTaskToBack(true);
                        finish();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
