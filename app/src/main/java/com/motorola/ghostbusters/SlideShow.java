package com.motorola.ghostbusters;

import android.app.Activity;
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
    //ProgressDialog mProgressDialog;
    //CollectData collectData;
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
    public static int gearsSetDone;
    //public static boolean isModeAuto;
    //public static boolean isModeDone;
    public static String imgUri;
    public static String imgUriDone;
    public static int max[];
    public static int mMax[];
    public static int mMin[];
    public static int mMaxIm[][];
    public static int mMinIm[][];
    public static String imgNamesToShow[];
    private int tmpN;
    private int tmpMax;
    private int tmpMin;
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
    public static boolean needSleep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.getIntent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //this.getIntent().setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_slide_show);
        mFlipper = (ViewFlipper) findViewById(R.id.mFlipper);
        myView = (ProgressLine) findViewById(R.id.progressLine);

        /*
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        int largeMemoryClass = am.getLargeMemoryClass();
        Log.d(TAG, "memoryClass:" + Integer.toString(memoryClass));
        Log.d(TAG, "largeMemoryClass:" + Integer.toString(largeMemoryClass));
        */

        imReadySemaphore = new Semaphore(0);

        mHandler = new Handler() {
            @Override
        public void handleMessage (Message msg) {
                //super.handleMessage(msg);
                //TODO
                if (msg.what < CYCLES) {
                    //try {
                    //imReadySemaphore.acquire();

                    imCounter = msg.what;
                    Log.d(TAG, "image counter = " + imCounter);
                    if (mCheck != imCounter || imCounter == 0) {
                        mFlipper.removeAllViews();

                        if (mBitmap != null) {
                            mBitmap.recycle();
                        }
                        mBitmap = null;
                        //System.gc();

                        if (imCounter == (CYCLES - 1) && MainActivity.currentUri != null) {
                            //setFlipperImages(2, false);
                            addSingleImage(MainActivity.currentUri);
                        //} else if (imCounter >= imgNames.length && imCounter < CYCLES && addCustom) {
                        } else if (imCounter >= standardImgMap.length && imCounter < CYCLES && addCustom) {
                            //setFlipperImages(1, false);
                            setFlipperImages(imCounter - standardImgMap.length, true);
                        }
                        //for (int k = 0; k < imgNames.length; k++) {
                        //    Log.d(TAG, "files: " + imgNames[k] + "; ");
                        //}
                        //if (imCounter < imgNames.length) {
                        if (imCounter < standardImgMap.length) {// && standardImgMap[imCounter][1].equals("1")) {
                            setFlipperImages(imCounter, false);

                            Log.d(TAG, "set image in map " + imCounter + ": " + standardImgMap[imCounter][0]);
                        }

                        Log.d(TAG, "image ready, setting it on display");
                        mFlipper.setDisplayedChild(0);

                        //lineToDraw = msg.what+1;
                        //lineToDraw = ++c;
                        //Log.d(TAG, "progress for line is " + lineToDraw + "/" + CYCLES);
                        //myView.setProgress(lineToDraw);
                        //myView.setCycles(CYCLES);
                        //myView.invalidate();
                        mCheck = imCounter;
                        imReadySemaphore.release();
                        tmpCount++;
                        Log.d(TAG, "semaphore is released after image " + imCounter);
                    }
                }

                else if (msg.what > 999) {

                    lineToDraw = tmpCount * (MainActivity.gearsCount+1) + (msg.what - 1000);
                    //Log.d(TAG, "progress for line is " + lineToDraw + "/" + CYCLES);
                    myView.setProgress(lineToDraw + 1);
                    //myView.setCycles(CYCLES);
                    myView.invalidate();
                    imReadySemaphore.release();
                    Log.d(TAG, "semaphore is released after progress update " + lineToDraw + " / " + ACTUAL_NUM * (MainActivity.gearsCount+1));
                }

                 else if (msg.what == FINISH) {
                    //mThreadForData.stop();
                    unBlockTouch();
                    isDone = true;
                    samplesDone = samples;
                    gearsSetDone = gearsSet;
                    imgUriDone = imgUri;
                    //isModeDone = isModeAuto;
                    //Log.d(TAG, "done");
                    mFlipper.removeAllViews();
                    if (mBitmap != null) {
                        mBitmap.recycle();
                    }
                    mBitmap = null;
                    //System.gc();

                    showResults();
                }
            }
        };

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = this.getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        Log.d(TAG, "screen width is " + screenWidth);

        userPref = PreferenceManager
                .getDefaultSharedPreferences(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.d(TAG, "call onResume");
        isDefault = false;
        c = 0;
        tmpCount = -1;
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
        gearsSet = userPref.getInt("gearsMask", 255);
        //isModeAuto = userPref.getBoolean("isAuto", true);
        imgUri = userPref.getString("uri", null);
        imgNames = getImgNames();


        if (imgNames != null) {
            ACTUAL_NUM = imgNames.size();
        } else {
            ACTUAL_NUM = 0;
        }

            CYCLES = MainActivity.standardEntries;

        Log.d(TAG, "cycles = " + CYCLES + "; act/num " + ACTUAL_NUM);
        if (addCustom) {
            customPath = userPref.getString("custom_path", "");
            addCustomImages(customPath);
            CYCLES += mFiles.size();
            ACTUAL_NUM += mFiles.size();
            Log.d(TAG, "added " + mFiles.size() + " images from custom folder");
        }
        Log.d(TAG, "image uri: " + MainActivity.currentUri);

        if (MainActivity.currentUri != null) {
            //addSingleImage(MainActivity.currentUri);
            CYCLES += 1;
            ACTUAL_NUM += 1;
        }

        Log.d(TAG, "isDone and isStopped: " + isDone + " / " + isStopped);

        if (standardImgMap == null || mMaxIm == null || CYCLES > mMaxIm.length
                || (imgUri != null && !imgUriDone.equals(imgUri)) || isStopped
                || samplesDone != samples || gearsSetDone != gearsSet) {
            Log.d(TAG, "we are here, cycles = " + CYCLES);
            isStopped = false;
            SharedPreferences.Editor editor = userPref.edit();
            editor.putBoolean("isStop", isStopped);
            editor.commit();
            standardImgMap = new String[MainActivity.standardEntries][3];
            standardImgMap = MainActivity.standardImgMap;
            mMaxIm = new int[CYCLES][TouchDevice.diagGearCount()+1];
            mMinIm = new int[CYCLES][TouchDevice.diagGearCount()+1];
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
            //imgMaskAsHex = getImgMask();
            //setFlipperImages(0, false);
        } else if (addCustom) {
            Log.d(TAG, "only custom path selected");
            //setFlipperImages(0, true);
        } else if (MainActivity.currentUri != null) {
            Log.d(TAG, "the only image is single");
            //addSingleImage(MainActivity.currentUri);
        }

        //mFlipper.setDisplayedChild(0);

        Log.d(TAG, "init cycles " + CYCLES);
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
            Log.d(TAG, "..." + MainActivity.standardImgMap[i][1]);
            if (MainActivity.standardImgMap[i][1].equals("1")) {
                imgCount++;
                imgs.add(MainActivity.standardImgMap[i][0]);
            }
        }

        /*

        Set<String> selections = userPref.getStringSet("advanced", null);
        String[] imgs = new String[selections.size()];
        imgs = selections.toArray(new String[]{});
        */

        return imgs;
    }

    public static String getImgMask() {
        //String[] bitsToAdd = new String [MainActivity.valSet.length];
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
            //standardImgMap[i][2] = "0";
            if (checked[i]) {
                maskAsBinaryString += "1";
                standardImgMap[i][1] = "1";
            } else {
                maskAsBinaryString += "0";
                standardImgMap[i][1] = "0";
            }
        }
        String imgMask = new StringBuilder(maskAsBinaryString).reverse().toString();
        Log.d(TAG, "string mask for images: " + maskAsBinaryString + "; length is " + maskAsBinaryString.length());
        int decimal = Integer.parseInt(imgMask,2);
        imgMaskAsString = "0x" + Integer.toString(decimal, 16);
        Log.d(TAG, "try to save mask as hex: " + imgMask + " -> " + imgMaskAsString);
        return imgMaskAsString;
    }

    private void setFlipperImages(int i, boolean isCustom) {
        ImageView mImage = new ImageView(getApplicationContext());
        BitmapFactory.Options mOpts = new BitmapFactory.Options();
        mOpts.inScaled = false;
        mOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        int drawableId;
        mBitmap = null;
        //Log.d(TAG, "bitmap is set to null");
        //mOpts.inBitmap = mBitmap;

        if (isCustom) {
            File imgFile = new File(mFiles.get(i));
            //Log.d(TAG, "img from custom path: " + imgFile.toString());
            imgNamesToShow[i+standardImgMap.length] = imgFile.toString();
            mBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), mOpts);
            mImage.setImageBitmap(mBitmap);

        } else {
            //drawableId = getResources().getIdentifier(imgNames[i], "raw", "com.motorola.ghostbusters");
            //Log.d(TAG, "img " + imgNames[i]);
            //imgNamesToShow[i] = imgNames[i];
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
        //mOpts.inPreferredConfig = Bitmap.Config.RGB_565;
        mBitmap = null;
        //mOpts.inBitmap = mBitmap;
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
            //unBlockTouch();
                /*
            for (int gear = 0; gear < MainActivity.gearsCount; gear++) {
                tmpMax = mMaxIm[0][gear];
                tmpMin = mMinIm[0][gear];
                for (int i = 1; i < CYCLES; i++) {
                    if (tmpMax < mMaxIm[i][gear]) {
                        tmpMax = mMaxIm[i][gear];
                    }
                    if (tmpMin < mMinIm[i][gear]) {
                        tmpMin = mMinIm[i][gear];
                    }
                }
                mMax[gear] = tmpMax;
                mMin[gear] = tmpMin;
                //Log.d(TAG, "total max/min for gear " + gear + ": " + mMax[gear] + "/" + mMin[gear]);
            }
            */
            Intent intent = new Intent(this,
                    ChartShow.class);
            //intent.putExtra("mMax", mMax);
            //intent.putExtra("mMin", mMin);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Toast.makeText(getApplicationContext(), "Test is done",
                    Toast.LENGTH_SHORT).show();
            //finish();
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
    }

    private void unBlockTouch() {

        TouchDevice.diagGearAuto(0);
        TouchDevice.diagEnableTouch();
    }

    private void saveDataToDropbox() {
        //TODO
    }

    public void onPause() {
        super.onPause();
        //TouchDevice.diagGearAuto(0);
        Log.d(TAG, "call onPause");
        //MainActivity.isGearsChanged = false;
        if (!isDone) {
            isStopped = true;
            unBlockTouch();
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
            //
            //while (!isInterrupted()) {
            Message msg;
            imCounter = 0;

            for (int i = 0; i < cycles; i++) {
                if (!isStopped) {

                try {
                    if ((i < standardImgMap.length && standardImgMap[i][1].equals("1") && standardImgMap[i][2].equals("0")) ||
                            i >= standardImgMap.length) {


                        msg = mHandler.obtainMessage(i);
                        mHandler.sendMessage(msg);
                        Log.d(TAG, "sent request to change image");
                        imReadySemaphore.acquire();

                        if (needSleep){
                            try {
                                Thread.sleep(10000);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            needSleep = false;
                        }

                        for (int gear = 0; gear <= MainActivity.gearsCount; gear++) {

                            msg = mHandler.obtainMessage(gear+1000);
                            mHandler.sendMessage(msg);
                            Log.d(TAG, "sent request to change progress");
                            imReadySemaphore.acquire();

                            if (gear == 0) {
                                Log.d(TAG, "sleeping to get image on screen...");
                                try {
                                    Thread.sleep(2000);

                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            Log.d(TAG, "start test for gear " + gear);
                            if (gear < MainActivity.gearsCount && String.valueOf(MainActivity.gearsEnabled[gear]).equals("0")) {
                                continue;
                            } else {
                                if (gear == MainActivity.gearsCount) {
                                    TouchDevice.diagGearAuto(0);
                                } else {
                                    TouchDevice.diagGearAuto(1);
                                    TouchDevice.diagGearSelect(gear);
                                }
                                /*
                                try {
                                    Thread.sleep(20);
                                    //Log.d(TAG, "sleeping 0.05 s...");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                */
                            }
/*
                            for (int j = 0; j < samples; j++) {

                                //tmpN = MainActivity.mDevice.diagPowerIM();
                                //long time1 = SystemClock.elapsedRealtime();

                                maxmin = MainActivity.mDevice.diagDeltaPeaks();
                                //long time2 = SystemClock.elapsedRealtime();
                               // Log.d(TAG, "time for 1 round: " + (time2-time1) + " ms");
                                //String value = "[ maxmin = "+ Integer.toString(maxmin) +
                                //        ", max = " +  Integer.toString(maxmin / 65536)  +
                                //        ", min = " + Integer.toString(maxmin % 65536) + "] ";
                                //tmpN = 100;
                                tmpMax = maxmin / 65536;
                                tmpMin = maxmin % 65536;
                                //tmpMax = 50;
                                //tmpMin = 45;
                                //if (tmpN > max[gear]) {
                                //    max[gear] = tmpN;
                                //}
                                if (tmpMax > mMaxIm[i][gear]) {
                                    mMaxIm[i][gear] = tmpMax;
                                }
                                if (tmpMin > mMinIm[i][gear]) {
                                    mMinIm[i][gear] = tmpMin;
                                }

                            }
                            */
                            maxmin = MainActivity.mDevice.diagDeltaPeaks(samples);
                            mMaxIm[i][gear] = maxmin / 65536;
                            mMinIm[i][gear] = maxmin % 65536;

                            Log.d(TAG, "max/min for image " + imgNamesToShow[i] + " for gear " + gear + ": " + mMaxIm[i][gear] + "/" + mMinIm[i][gear]);

                        }
                        if (i < standardImgMap.length) {
                            standardImgMap[i][2] = "1";
                            Log.d(TAG, "Setting tested value to true for image " + i);
                        }
                    } else if (i < standardImgMap.length && (standardImgMap[i][1].equals("0")
                            && standardImgMap[i][2].equals("0"))) {
                        standardImgMap[i][2] = "0";
                        Log.d(TAG, "Setting tested value to false for image " + i);
                    }
                } catch (InterruptedException e) {
                    //Log.d(TAG, "interrupt");
                    e.printStackTrace();
                }
                }
        }
                msg = mHandler.obtainMessage(FINISH);
                mHandler.sendMessage(msg);
        //}
        }

    }
///////////////////

}
