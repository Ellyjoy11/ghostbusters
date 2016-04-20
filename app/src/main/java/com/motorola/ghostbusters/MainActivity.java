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
    public static boolean isRxEnabled;
    public static boolean isTxEnabled;

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

        mDevice = new TouchDevice();
        mDevice.diagInit(touchFWPath);
        gearsCount = mDevice.diagGearCount();

        gearsNames = new String[gearsCount + 1];
        selectedGear = new boolean[gearsCount + 1];
        gearsNames[0] = "Automatic";
        selectedGear[0] = userPref.getBoolean("isAuto", true);
        isModeAuto = userPref.getBoolean("isAuto", true);
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

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        int largeMemoryClass = am.getLargeMemoryClass();
        //Log.d(TAG, "memoryClass:" + Integer.toString(memoryClass));
        //Log.d(TAG, "largeMemoryClass:" + Integer.toString(largeMemoryClass));

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

        SharedPreferences.Editor editor = userPref.edit();
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

        //check if custom path to addl images entered and correct
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
            Intent intent = new Intent(this, ChartShow.class);
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
        Intent intent = new Intent(getApplicationContext(),
                com.motorola.ghostbusters.SlideShow.class);
        startActivity(intent);
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


}
