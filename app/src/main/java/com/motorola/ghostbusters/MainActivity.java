package com.motorola.ghostbusters;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;


public class MainActivity extends Activity {
    public static final String ABOUT_TITLE = "\u00a9 2015-2016\n" +
            "Igor Kovalenko\nElena Last\nAlex Filonenko\nKonstantin Makariev";
    public static String ABOUT_VERSION;
    private final static String TAG = "Ghostbusters";

    public static String touchFWPath;
    public static String product;
    public static String barcode;
    public static String touchCfg;
    public static String productInfo;

    SharedPreferences userPref;
    public static boolean addCustom = false;
    public static String customPath = "";
    public static int screenWidth;
    public static int screenHeight;

    public static String[] entrySet;
    public static String[] valSet;
    public static String[] defSet;
    public static int standardEntries;

    public static int gearsCount;
    private static final int READ_REQUEST_CODE = 42;
    public static Uri currentUri = null;

    public static TouchDevice mDevice;
    public static char[] gearsEnabled;

    public static int threshold;
    public static int satLevel;
    public static int hysteresis;
    public static int TxThreshold;
    public static int RxThreshold;

    public boolean selectedGear[];
    public boolean selectedImg[];
    public static char[] imgSelected;

    public String[] gearsNames;
    static ImageButton btnGear;
    public static boolean isModeAuto;

    public static String maskAsBinaryString;
    public static String imgMaskAsString;
    public static String[][] standardImgMap;
    public static String mask;
    public static String panel = "";

    ImageButton addButton;
    TextView testInfo;
    TextView prodInfo;
    ImageView logoView;

    private static File[] list1;
    private static File[] list2;

    public static String testType;
    //public static boolean isCycleTest = true;
    public static boolean isRxEnabled;
    public static boolean isTxEnabled;
    public static int baseStretch;

    public static int mMaxImC[][][];
    public static int mMinImC[][][];

    public static int mMaxRxImC[][][];
    public static int mMinRxImC[][][];

    public static int mMaxTxImC[][][];
    public static int mMinTxImC[][][];

    public static int eventCountReport2[][];
    public static int eventCountReport59[][];

    public static int intTime2[];
    public static int intTimeBase2;
    public static int intTime59[];
    public static int filterBw[];
    public static int intTimeBase59;
    public static int intTimeRange;
    public static int filterBwRange;
    public static int TEST_CYCLES;
    public static int stretches;

    public static int intTimeBase2default;
    public static int intTimeBase59default;
    public static int filterBwBase;

    public static int bwStart;
    public static int bwEnd;

    public static String initialC95FilterBwBurstLen = "";
    public static String initialBurstLen = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoView = (ImageView) findViewById(R.id.logoView);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        userPref = PreferenceManager
                .getDefaultSharedPreferences(this);

        String appVersion = "";
        try {
            appVersion = this.getPackageManager().getPackageInfo(
                    this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "App version not found " + e.getMessage());
        }

        ABOUT_VERSION = "Ghostbusters v." + appVersion;

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = this.getWindowManager();
        wm.getDefaultDisplay().getMetrics(displaymetrics);

        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;
        Log.d(TAG, "screen resolution: " + screenWidth + "x" + screenHeight);

        setMultiList(screenWidth);
        standardEntries = valSet.length;

        if (standardImgMap == null) {

            standardImgMap = new String[standardEntries][3];

            for (int i = 0; i < standardEntries; i++) {
                standardImgMap[i][2] = "0";
            }
        }
        addButton = (ImageButton) findViewById(R.id.addButton);
        if (currentUri == null) {
            addButton.setImageResource(R.drawable.add_image);
        } else {
            addButton.setImageResource(R.drawable.gallery);
        }

        testInfo = (TextView) findViewById(R.id.testInfo);
        prodInfo = (TextView) findViewById(R.id.productInfo);
        touchFWPath = getTouchFWPath();

        if (mDevice == null) {
            mDevice = new TouchDevice();
            mDevice.diagInit(touchFWPath);
            Log.d(TAG, "diagInit called");
        }
        gearsCount = mDevice.diagGearCount();

    }


    @Override
    public void onResume() {
        super.onResume();

        if (!checkSELinuxStatus() || !checkPermissions(touchFWPath)) {
            AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
            builder.setMessage(
                    "Not enough permissions to run the test!\n"
                            + "Fix permissions and try again")
                    .setTitle("Oops...");

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moveTaskToBack(true);
                        }
                    });
            builder.setNegativeButton("Ignore",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
/*
        isHWresetRequired = userPref.getBoolean("need_reset", false);

        if (isHWresetRequired) {
            errorDialog();
        }
*/
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        int largeMemoryClass = am.getLargeMemoryClass();
        //Log.d(TAG, "memoryClass:" + Integer.toString(memoryClass));
        //Log.d(TAG, "largeMemoryClass:" + Integer.toString(largeMemoryClass));

        intTimeBase2default = mDevice.diagTranscapIntDur();
        Log.d(TAG, "TranscapIntDur:" + Integer.toString(intTimeBase2default));
        intTimeBase59default = mDevice.diagHybridIntDur();
        Log.d(TAG, "defaults are " + intTimeBase2default + " and " + intTimeBase59default);
        filterBwBase = getC95FilterBwBurstLen();

        SharedPreferences.Editor editor = userPref.edit();

        editor = userPref.edit();
        editor.putString("int_base2", Integer.toString(intTimeBase2default));
        editor.putString("int_base59", Integer.toString(intTimeBase59default));
        editor.putString("bw_base", Integer.toString(filterBwBase)); //TBD Integer.toString(intTimeBase59default));
        if (!userPref.getBoolean("intBasesSet", false)) {
            editor.putString("stretches", Integer.toString(gearsCount + 1));
            //editor.putString("bw_base", "0");
            editor.putBoolean("intBasesSet", true);
        }
        editor.commit();

        gearsNames = new String[gearsCount + 1];
        selectedGear = new boolean[gearsCount + 1];
        gearsNames[0] = "Automatic";
        selectedGear[0] = userPref.getBoolean("isAuto", true);
        isModeAuto = userPref.getBoolean("isAuto", true);

        testType = userPref.getString("test_type", "Report 2");
        if (testType.contains("59") && mDevice.diagHasHybridBaselineControl() != 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
            builder.setMessage(
                    "Report 59 is not supported in device SW")
                    .setTitle("Oops...");

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getApplicationContext(),
                                    com.motorola.ghostbusters.SetPreferences.class);
                            startActivity(intent);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
/*
        intTimeRange = Integer.parseInt(userPref.getString("int_time", "0"));
        filterBwRange = Integer.parseInt(userPref.getString("bw_range", "0"));
        if (intTimeRange >= 0) {
            TEST_CYCLES = 2 * intTimeRange + 1;
        }
        if (filterBwRange > 0 && intTimeRange == 0) {
            filterBwBase = Integer.parseInt(userPref.getString("bw_base", "0"));

            if (filterBwBase - filterBwRange < 0 ) {
                bwStart = 0;
            } else {
                bwStart = filterBwBase - filterBwRange;
            }
            if (filterBwBase + filterBwRange > 7) {
                bwEnd = 7;
            } else {
                bwEnd = filterBwBase + filterBwRange;
            }
            TEST_CYCLES = bwEnd - bwStart +1;
        }
        */
        //Log.d(TAG, "test cycles to run: " + TEST_CYCLES + ".. " + filterBwRange);
        int intbase2 = Integer.parseInt(userPref.getString("int_base2", Integer.toString(intTimeBase2default)));
        int intbase59 = Integer.parseInt(userPref.getString("int_base59", Integer.toString(intTimeBase59default)));

        if ( intbase2 - intTimeRange < 1 ||
                intbase59 - intTimeRange < 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
            builder.setMessage(
                    "Range is too big, IntDur can't be negative! " + intbase2 + " " + intbase59)
                    .setTitle("Oops...");

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getApplicationContext(),
                                    com.motorola.ghostbusters.SetPreferences.class);
                            startActivity(intent);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        stretches = Integer.parseInt(userPref.getString("stretches", Integer.toString(gearsCount + 1)));
        intTimeRange = Integer.parseInt(userPref.getString("int_time", "0"));
        filterBwRange = Integer.parseInt(userPref.getString("bw_range", "0"));

        if (intTimeRange > 0 && filterBwRange > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
            builder.setMessage(
                    "Both sweep test ranges are non-zero.\nSweep test will be run for IntDur.\n" +
                            "To run filter BW sweep test\nset IntDur range to 0.")
                    .setTitle("Are you aware?");

            builder.setPositiveButton("Settings",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getApplicationContext(),
                                    com.motorola.ghostbusters.SetPreferences.class);
                            startActivity(intent);
                        }
                    });
            builder.setNegativeButton("Sweep IntDur",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        /*

        if (intTime2 == null) {
            intTime2 = new int[TEST_CYCLES];
        }
        if (intTime59 == null) {
            intTime59 = new int[TEST_CYCLES];
        }
        if (filterBw == null) {
            filterBw = new int[TEST_CYCLES];
        }

        if (!userPref.getBoolean("report2_data_exists", false) || TEST_CYCLES != userPref.getInt("cycles_done_2", 0) || mMaxImC == null) {
            mMaxImC = new int[TEST_CYCLES][standardEntries + 50][mDevice.diagGearCount() + 1];
            mMinImC = new int[TEST_CYCLES][standardEntries + 50][mDevice.diagGearCount() + 1];
            eventCountReport2 = new int[TEST_CYCLES][mDevice.diagGearCount() + 1];
        }
        if (!userPref.getBoolean("report59_data_exists", false) || TEST_CYCLES != userPref.getInt("cycles_done_59", 0) ||
        stretches > userPref.getInt("stretches_done", 0) || mMaxRxImC == null) {
            mMaxRxImC = new int[TEST_CYCLES][standardEntries + 50][stretches];
            mMinRxImC = new int[TEST_CYCLES][standardEntries + 50][stretches];
            mMaxTxImC = new int[TEST_CYCLES][standardEntries + 50][stretches];
            mMinTxImC = new int[TEST_CYCLES][standardEntries + 50][stretches];
            eventCountReport59 = new int[TEST_CYCLES][stretches];
        }
        */

        editor = userPref.edit();
        editor.putInt("cycle_counter", 0);
        editor.commit();

        product = Build.DEVICE;
        barcode = Build.SERIAL;
        touchCfg = getTouchCfg();
        panel = getPanelType();

        String textForProductInfo = "Product: " + productInfo + "  Config: " + touchCfg;
        if (!panel.isEmpty()) {
            textForProductInfo += " Panel: " + panel;
        }
        prodInfo.setText(textForProductInfo);
        //Log.d(TAG, "product " + product + "; config ID: " + touchCfg + "; panel: " + panel);

        String patternShort = "(?i)(\\w+)(_|-).*";
        if (product.matches(patternShort)) {
            product = product.replaceAll(patternShort, "$1");
        }
        //Log.d(TAG, "product " + product + "; config ID: " + touchCfg);

        int gears = mDevice.diagGearsEnabled();

        String tmp = String.format("%16s", Integer.toBinaryString(gears)).replace(' ', '0');
        Log.d(TAG, "gears " + gearsCount + "; gearsEnabled: " + tmp);
        char[] gearsEnabledTmp = tmp.toCharArray();
        gearsEnabled = new char[gearsCount];
        //int diffL = gearsEnabledTmp.length - gearsCount;

        for (int i=0; i < gearsCount; i++) {
            gearsEnabled[i] = gearsEnabledTmp[gearsEnabledTmp.length - i - 1];
            Log.d(TAG, "gear "+i+" enabled: " + gearsEnabled[i]);
        }

        editor = userPref.edit();
        StringBuilder str = new StringBuilder("");
        str.append(MainActivity.gearsEnabled);
        String toSave = str.reverse().toString();
        if (!toSave.isEmpty()) {
            Log.d(TAG, "gears mask: " + toSave);
            TouchDevice.diagEnableGears(Integer.parseInt(toSave, 2));
            TouchDevice.diagForceUpdate();
            Log.d(TAG, "try to save gears mask as int: " + Integer.parseInt(toSave, 2));
            editor.putInt("gearsMask", Integer.parseInt(toSave, 2));
            editor.commit();
        }

        selectedImg = new boolean[standardEntries];
        imgSelected = new char[standardEntries];
        char[] zeroMask = new char[standardEntries];
        Arrays.fill(zeroMask, '1');
        mask = userPref.getString("imgMask", new String(zeroMask));
        editor = userPref.edit();
        if (mask != null) {
            for (int k = 0; k < standardEntries; k++) {
                if (mask.charAt(k) == '1') {
                    selectedImg[k] = true;
                    imgSelected[k] = '1';
                    standardImgMap[k][0] = valSet[k];
                    standardImgMap[k][1] = "1";
                } else {
                    selectedImg[k] = false;
                    imgSelected[k] = '0';
                    standardImgMap[k][0] = valSet[k];
                    standardImgMap[k][1] = "0";
                }
            }
        } else {
            mask = "";
            for (int k = 0; k < standardEntries; k++) {
                selectedImg[k] = true;
                imgSelected[k] = '1';
                standardImgMap[k][0] = valSet[k];
                standardImgMap[k][1] = "1";
            }
            editor.putBoolean("isListSet", true);
        }

        StringBuilder str1 = new StringBuilder("");
        str1.append(imgSelected);
        mask = str1.toString();
        String imgMask = str1.reverse().toString();
        int decimal = Integer.parseInt(imgMask, 2);
        imgMaskAsString = "0x" + Integer.toString(decimal, 16);
        Log.d(TAG, "img mask: " + imgMaskAsString);

        editor.putString("imgMask", mask);
        editor.putString("hexMask", imgMaskAsString);
        editor.commit();

        for (int i=0; i < gearsCount; i++) {

            if (String.valueOf(gearsEnabled[i]).equals("1")) {
                selectedGear[i+1]=true;
            } else {
                selectedGear[i+1]=false;
            }
            gearsNames[i+1] = "Gear " + (i);
        }

        btnGear = (ImageButton) findViewById(R.id.gearsButton);


        threshold = mDevice.diagFingerThreshold();
        satLevel = mDevice.diagSaturationLevel();
        hysteresis = mDevice.diagFingerHysteresis();
        //Log.d(TAG, "getValues: " + threshold + "; " + satLevel + "; " + hysteresis);

//DO NOT remove second reading of values!!!
        threshold = mDevice.diagFingerThreshold();
        satLevel = mDevice.diagSaturationLevel();
        hysteresis = mDevice.diagFingerHysteresis();
        //Log.d(TAG, "getValues #2: " + threshold + "; " + satLevel + "; " + hysteresis);

        //TxThreshold = Integer.parseInt(userPref.getString("tx", "40"));
        //RxThreshold = Integer.parseInt(userPref.getString("rx", "100"));

        TxThreshold = mDevice.diagTxObjThresh();
        RxThreshold = mDevice.diagRxObjThresh();
        Log.d(TAG, "Report 59 thresholds: Tx=" + TxThreshold + "; Rx=" + RxThreshold);

        if (mDevice.diagEnHybridOnRx() == 1) {
            isRxEnabled = true;
        } else {
            isRxEnabled = false;
        }

        if (mDevice.diagEnHybridOnTx() == 1) {
            isTxEnabled = true;
        } else {
            isTxEnabled = false;
        }
        Log.d(TAG, "Rx and Tx enabled: " + isRxEnabled + "; " + isTxEnabled);

        baseStretch = mDevice.diagHybridStretchDur();
        Log.d(TAG, "read base stretch: " + baseStretch);

//check if custom path to addl images entered and correct
        addCustom = userPref.getBoolean("custom", false);
        if (addCustom) {
            customPath = userPref.getString("custom_path", "");
            if (!checkCustomPath(customPath)) {
                AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
                builder.setMessage(
                        "Path you entered is not valid!\n"
                                + "Change the path or uncheck the option to add custom images")
                        .setTitle("Oops...");

                builder.setPositiveButton("Settings",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(getApplicationContext(),
                                        com.motorola.ghostbusters.SetPreferences.class);
                                startActivity(intent);
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
                Log.d(TAG, "custom Path is not valid");
            } else {
                Log.d(TAG, "custom Path: " + customPath);
            }
        } else {
            Log.d(TAG, "add custom images option is not checked");
        }
        updateTextInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        if (item.getItemId() == R.id.action_show_stats) {
            Intent intent = new Intent(this, PieChartShow.class);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.action_show_chart) {
            Intent intent = new Intent(this, CycleTestChart.class);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, com.motorola.ghostbusters.SetPreferences.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_CustDialog));

            builder.setMessage(ABOUT_TITLE).setTitle(
                    ABOUT_VERSION);

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    public void onPause() {
        super.onPause();
        System.gc();
    }

    public void onStartClick(View view) {
        if (mDevice.diagTranscapIntDur() - intTimeRange < 1 ||
                mDevice.diagHybridIntDur() - intTimeRange < 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
            builder.setMessage(
                    "Range is too big, IntDur can't be negative!")
                    .setTitle("Oops...");

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getApplicationContext(),
                                    com.motorola.ghostbusters.SetPreferences.class);
                            startActivity(intent);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            initArrays();
            Intent intent = new Intent(getApplicationContext(),
                    com.motorola.ghostbusters.SlideShow.class);
            startActivity(intent);
        }
    }

    public void onRptClick(View view) {
        Intent intent = new Intent(getApplicationContext(),
                com.motorola.ghostbusters.TouchRpt.class);
        startActivity(intent);
    }

    public static String getTouchFWPath() {

        String touchPath = "/sys/bus/i2c/devices/";// +"2-0020";
        String exactFolder = "";
        String fwPattern1 = "(?i).*(\\w{1}-\\w{4}).*";

        File rootSyna = new File("/sys/bus/i2c/drivers/synaptics_dsx_i2c");
        list1 = rootSyna.listFiles();
        File rootAtm = new File("/sys/bus/i2c/drivers/atmel_mxt_ts");
        list2 = rootAtm.listFiles();

        if (list1 != null) {

            for (File f : list1) {
                if (f.isDirectory()) {
                    Log.d(TAG, "checking: " + f.toString());
                    if (f.toString().matches(fwPattern1)) {
                        exactFolder = f.toString().replaceAll(fwPattern1, "$1");
                    }
                }
            }
        }
        if (list2 != null && exactFolder == "") {
            for (File f : list2) {
                if (f.isDirectory()) {
                    Log.d(TAG, "checking: " + f.toString());
                    if (f.toString().matches(fwPattern1)) {
                        exactFolder = f.toString().replaceAll(fwPattern1, "$1");
                    }
                }
            }
        }

        touchPath += exactFolder;
        Log.d(TAG, "determined touch fw path: " + touchPath);
        return touchPath;
    }

    public static String getTouchCfg() {
        String touchCfg = "";
        String catIC = "";
        catIC = readFile(touchFWPath + "/ic_ver");
        String cfgPattern = "(?i).*Config\\s+ID:\\s+(\\w+).*";
        String prodInfo = "(?i).*Product\\s+ID:\\s+([\\w|\\(|\\)]+)Build.*";

        if (catIC.matches(prodInfo)) {
            productInfo = catIC.replaceAll(prodInfo, "$1");
        }
        if (catIC.matches(cfgPattern)) {
            touchCfg = catIC.replaceAll(cfgPattern, "$1");
        }
        //Log.d(TAG, "product info: " + productInfo);
        return touchCfg;
    }

    public static String readFile(String fileToRead) {
        FileInputStream is;
        BufferedReader reader;
        String readOut = "";
        String line;
        final File file = new File(fileToRead);

        if (file.exists()) {
            try {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                //line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    //Log.d(TAG, "read from file: " + line);
                    readOut += line;
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return readOut;
    }

    public static String getPanelType() {
        //TODO
        String panel = readFile("/sys/class/graphics/fb0/panel_supplier");
        String tmpVersion = readFile("/sys/class/graphics/fb0/panel_ver");
        if (!panel.isEmpty() && !tmpVersion.isEmpty()) {
            panel += "-v" + tmpVersion.substring(tmpVersion.length() - 3, tmpVersion.length() - 2); // begin index inclusive, end index exclusive!!!
        }
        return panel;
    }

    public static String decToHex(String decToConvert) {
        String res = "";
        try {
            int i = Integer.parseInt(decToConvert);
            res = Integer.toString(i, 16);
            //Log.d(TAG, "parsing dec: " + decToConvert + "; get hex: " + res);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return res;
    }

    private static int hexToDec(String hexToConvert) {
        int res = 0;
        try {
            res = Integer.parseInt(hexToConvert, 16);
            //Log.d(TAG, "parsing hex: " + hexToConvert + "; get dec: " + res);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static int updateFile(String fileToUpdate, String value) {

        final File file = new File(fileToUpdate);

        try {
            File myFile = new File(fileToUpdate);

            //myFile.createNewFile();

            //Log.d(TAG, "file exist " + myFile.exists());
            //Log.d(TAG, "file read, write, exec: " + myFile.canRead() + myFile.canWrite() + myFile.canExecute());
            myFile.setWritable(true);
            //Log.d(TAG, "file read, write, exec: " + myFile.canRead() + myFile.canWrite() + myFile.canExecute());
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(value);
            myOutWriter.close();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
            return 0;
        }
        return 1;
    }

    public static boolean checkCustomPath(String customPath) {
        boolean res = false;
        File file = new File(customPath);
        //Log.d(TAG, customPath + " file exist? " + file.exists() + " isDir? " + file.isDirectory());
        if (file.exists() && file.isDirectory()) {
            //Log.d(TAG, "file exist? " + file.exists() + " isDir? " + file.isDirectory());
            return true;
        } else {
            return false;
        }
    }

    private void setMultiList(int resolution) {

        Log.d(TAG, "setting defaults for resolution: " + resolution);

        switch (resolution) {
            case 720:
                entrySet = getResources().getStringArray(
                        R.array.drawablesArray720);
                valSet = getResources().getStringArray(
                        R.array.drawablesValues720);
                defSet = getResources().getStringArray(
                        R.array.drawablesDefaults720);
                break;
            case 1080:
                entrySet = getResources().getStringArray(
                        R.array.drawablesArray1080);
                valSet = getResources().getStringArray(
                        R.array.drawablesValues1080);
                defSet = getResources().getStringArray(
                        R.array.drawablesDefaults1080);
                break;
            case 1440:
                entrySet = getResources().getStringArray(
                        R.array.drawablesArray1440);
                valSet = getResources().getStringArray(
                        R.array.drawablesValues1440);
                defSet = getResources().getStringArray(
                        R.array.drawablesDefaults1440);
                break;
            default:
                entrySet = getResources().getStringArray(
                        R.array.drawablesArray720);
                valSet = getResources().getStringArray(
                        R.array.drawablesValues720);
                defSet = getResources().getStringArray(
                        R.array.drawablesDefaults720);
                break;
        }
    }

    public static int getGearsCount() {
        return mDevice.diagGearCount();
    }


    /////////////

    public void onAddFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        //intent.set
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        //Log.d(TAG, "result is "+resultCode+"; request Code is " + requestCode);

        if (resultCode == Activity.RESULT_OK)
        {
            //Log.d(TAG, "result OK ");
           if (requestCode == READ_REQUEST_CODE)
               //Log.d(TAG, "request code is read" + "result data is " + resultData.getData().toString());
            {
                if (resultData != null) {
                        currentUri = resultData.getData();
                        Log.d(TAG, "file " +currentUri.toString());
                    SharedPreferences.Editor editor = userPref.edit();
                    editor.putString("uri", currentUri.toString());
                    editor.commit();
                    addButton.setImageResource(R.drawable.gallery);
                } else {
                    Log.d(TAG, "add image fail");
                }
            }

        } else {
            SharedPreferences.Editor editor = userPref.edit();
            currentUri = null;
            editor.putString("uri", null);
            editor.commit();
            addButton.setImageResource(R.drawable.add_image);
        }
    }

    //////////

    public void onImSetClick(View view) {


        final AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
        builder.setMultiChoiceItems(entrySet, selectedImg, new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialogInterface, int item, boolean b) {
                Log.d(TAG, String.format("%s: %s", entrySet[item], b));
                //////////

                    maskAsBinaryString = "";
                    for (int j = 0; j < selectedImg.length; j++) {
                        if (selectedImg[j] == true) {
                            imgSelected[j] = '1';
                            standardImgMap[j][0] = valSet[j];
                            standardImgMap[j][1] = "1";
                        } else {
                            imgSelected[j] = '0';
                            standardImgMap[j][0] = valSet[j];
                            standardImgMap[j][1] = "0";
                        }

                    }
                StringBuilder str = new StringBuilder("");
                str.append(imgSelected);
                maskAsBinaryString = str.toString();
                String imgMask = str.reverse().toString();
                int decimal = Integer.parseInt(imgMask, 2);
                imgMaskAsString = "0x" + Integer.toString(decimal, 16);
                SharedPreferences.Editor editor = userPref.edit();

                Log.d(TAG, "try to save mask as hex: " + imgMask + " -> " + imgMaskAsString);
                editor.putString("imgMask", maskAsBinaryString);
                editor.putString("hexMask", imgMaskAsString);
                editor.commit();
                updateTextInfo();
                    //builder.setTitle("Current set: " + imgMaskAsString);

                ///////////
            }
        });

        builder.setTitle("Current set of images");// + imgMaskAsString);
        //builder.setMessage(imgMaskAsString);

        AlertDialog alertdialog=builder.create();
        alertdialog.show();

    }

    public void updateTextInfo() {
        String infoText = "Set of images: " + userPref.getString("hexMask", null);
        if (addCustom) {
            infoText += " + " + customPath;
        }
        if (currentUri != null) {
            infoText += " + user image";
        }
        infoText += " Test type: " + testType;
        testInfo.setText(infoText);
    }

    public void onGearClick(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
        builder.setTitle("Select Gear");
        builder.setMultiChoiceItems(gearsNames, selectedGear, new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialogInterface, int item, boolean b) {
                Log.d(TAG, String.format("%s: %s", gearsNames[item], b));
                //////////

                if (item != 0) {
                    for (int j = 1; j < selectedGear.length; j++) {
                        if (selectedGear[j] == true) {
                            gearsEnabled[j - 1] = '1';
                        } else {
                            gearsEnabled[j - 1] = '0';
                        }

                    }
                    StringBuilder str = new StringBuilder("");
                    str.append(MainActivity.gearsEnabled);
                    String toSave = str.reverse().toString();
                    Log.d(TAG, "gears mask: " + toSave);
                    TouchDevice.diagEnableGears(Integer.parseInt(toSave, 2));
                    TouchDevice.diagForceUpdate();
                    Log.d(TAG, "try to save gears mask as int: " + Integer.parseInt(toSave, 2));
                    SharedPreferences.Editor editor = userPref.edit();
                    editor.putInt("gearsMask", Integer.parseInt(toSave, 2));
                    editor.commit();
                    //isGearsChanged = true;
                } else {
                    if (selectedGear[0] == true) {
                        TouchDevice.diagGearAuto(0);
                        isModeAuto = true;
                    } else {
                        TouchDevice.diagGearAuto(1);
                        isModeAuto = false;
                    }
                    SharedPreferences.Editor editor = userPref.edit();
                    editor.putBoolean("isAuto", isModeAuto);
                    editor.commit();
                    TouchDevice.diagForceUpdate();
                }
                ///////////
            }
        });

        AlertDialog alertdialog=builder.create();
        alertdialog.show();

    }

/*
    public class DialogSelectionClickHandler implements
            DialogInterface.OnMultiChoiceClickListener {
        public void onClick(DialogInterface dialog, int clicked,
                            boolean selected) {

            // Log.i("ME", _options[clicked] + " selected: " + selected);
        }
    }
*/

    private static boolean checkSELinuxStatus() {
        String cmd_out = "";
        String line;
        boolean status = false;
        try {
            Process pr = Runtime.getRuntime().exec("getprop");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    pr.getInputStream()));
            while ((line = in.readLine()) != null) {
                cmd_out += line;
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //String pattern = "(?i).*permissive.*";
        if (cmd_out.contains("[ro.boot.selinux]: [permissive]") || cmd_out.contains("[ro.boot.selinux]: [disabled]")) {
            status = true;
        }
        //Log.d(TAG, "cmd: " + cmd_out + " // result: " + status);
        return status;
    }

    public static boolean checkPermissions(String touchFWPath) {
        //boolean res = false;
        File file = new File(touchFWPath + File.separator + "reporting");
        if (file.exists()) {
            Log.d(TAG, "reporting file permissions: " + file.canRead() + file.canWrite() + file.canExecute());
            if (file.canRead() && file.canWrite()) {
                return true;
            } else {
                return false;
            }
        } else {
            Log.d(TAG, "reporting file does not exist");
            return false;
        }
    }

    public void onDisplayTest(View view) {

        Intent intent = new Intent(getApplicationContext(),
                com.motorola.ghostbusters.DisplayTest.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        //Log.d(TAG, "Destroy method is called");
        SharedPreferences.Editor editor = userPref.edit();
        editor.putInt("cycle_counter", 0);
        editor.putInt("cycles_done_2", 0);
        editor.putInt("cycles_done_59", 0);
        editor.putInt("stretches_done", 0);
        editor.commit();
        //mDevice.diagResetTouch();
        super.onDestroy();
    }

    /*
    @Override
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
    */

    public static int getC95FilterBwBurstLen () {
        int filterBwValue = 0;

        String toParse = readFile(touchFWPath + "/f54/c95_filter_bw");
        String burstLen = readFile(touchFWPath + "/f54/c95_first_burst_length_lsb");
        if (initialC95FilterBwBurstLen.isEmpty()) {
            initialC95FilterBwBurstLen = toParse;
            initialBurstLen = burstLen;
        }
        Log.d(TAG, "read from filter BW reg: " + toParse);
        if (!toParse.isEmpty()) {
            String[] stringValues = toParse.split(" ");
            int[] filterBwIntValues = new int[stringValues.length];
            for (int i = 0; i < filterBwIntValues.length; i++) {
                filterBwIntValues[i] = Integer.parseInt(stringValues[i]);
            }
            int currGear = Integer.parseInt(readFile(touchFWPath + "/f54/d17_freq"));
            Log.d(TAG, "current gear is " + currGear);
            filterBwValue = filterBwIntValues[currGear];
        }
        Log.d(TAG, "Base filter BW value = " + filterBwValue);
        return filterBwValue;
    }

    public static void restoreFilterBw () {
        updateFile(touchFWPath + "/f54/c95_filter_bw", initialC95FilterBwBurstLen);
        updateFile(touchFWPath + "/f54/c95_first_burst_length_lsb", initialBurstLen);
        //mDevice.diagForceUpdate();
        Log.d(TAG, "/f54/c95_filter_bw written back to " + initialC95FilterBwBurstLen);
        Log.d(TAG, "/f54/c95_first_burst_length_lsb written back to " + initialBurstLen);
    }

    public void initArrays () {

        if (intTimeRange >= 0) {
            TEST_CYCLES = 2 * intTimeRange + 1;
        }
        if (filterBwRange > 0 && intTimeRange == 0) {
            //filterBwBase = Integer.parseInt(userPref.getString("bw_base", "0"));
            filterBwBase = getC95FilterBwBurstLen();
            //mDevice.diagSetC95FilterBwBurstLen(filterBwBase);

            if (filterBwBase - filterBwRange < 0 ) {
                bwStart = 0;
            } else {
                bwStart = filterBwBase - filterBwRange;
            }
            if (filterBwBase + filterBwRange > 7) {
                bwEnd = 7;
            } else {
                bwEnd = filterBwBase + filterBwRange;
            }
            TEST_CYCLES = bwEnd - bwStart +1;
            Log.d(TAG, "filter BW base = " + filterBwBase + "; range = " + filterBwRange);
            Log.d(TAG, "filter BW range is from " + bwStart + " to " + bwEnd);
        }


        if (intTime2 == null) {
            intTime2 = new int[TEST_CYCLES];
        }
        if (intTime59 == null) {
            intTime59 = new int[TEST_CYCLES];
        }
        if (filterBw == null) {
            filterBw = new int[TEST_CYCLES];
        }

        //SharedPreferences.Editor editor = userPref.edit();

        //editor = userPref.edit();

        if (!userPref.getBoolean("report2_data_exists", false) || TEST_CYCLES != userPref.getInt("cycles_done_2", 0) || mMaxImC == null) {
            mMaxImC = new int[TEST_CYCLES][standardEntries + 50][mDevice.diagGearCount() + 1];
            mMinImC = new int[TEST_CYCLES][standardEntries + 50][mDevice.diagGearCount() + 1];
            eventCountReport2 = new int[TEST_CYCLES][mDevice.diagGearCount() + 1];
           // if (bwEnd > 0) {
           //     editor.putBoolean("filterBW_2_done", false);
           // }
        }
        if (!userPref.getBoolean("report59_data_exists", false) || TEST_CYCLES != userPref.getInt("cycles_done_59", 0) ||
                stretches > userPref.getInt("stretches_done", 0) || mMaxRxImC == null) {
            mMaxRxImC = new int[TEST_CYCLES][standardEntries + 50][stretches];
            mMinRxImC = new int[TEST_CYCLES][standardEntries + 50][stretches];
            mMaxTxImC = new int[TEST_CYCLES][standardEntries + 50][stretches];
            mMinTxImC = new int[TEST_CYCLES][standardEntries + 50][stretches];
            eventCountReport59 = new int[TEST_CYCLES][stretches];
            //if (bwEnd > 0) {
            //    editor.putBoolean("filterBW_59_done", false);
            //}
        }
        //editor.commit();

    }


}
