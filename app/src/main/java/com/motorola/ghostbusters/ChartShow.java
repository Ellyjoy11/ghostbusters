package com.motorola.ghostbusters;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;


public class ChartShow extends Activity {
    MyChart chartView;
    public static final String TAG = "Ghostbusters";
    public static SharedPreferences userPref;
    public static int[] maxV;
    public static int[] minV;
    public static int mMaxImV[][];
    public static int mMinImV[][];
    private static int tmpMax;
    private static int tmpMin;
    public static String[] imgNames;
    public static String imgMaskAsHex = "";
    public static String maskAsBinaryString;
    public static String[][] standardImgMap;
    public static boolean runFromChart = false;
    public static boolean addCustom;
    public static boolean needRetest;
    TextView touchInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_show);
        chartView = (MyChart) findViewById(R.id.myChart);

        userPref = PreferenceManager
                .getDefaultSharedPreferences(this);
        standardImgMap = new String[MainActivity.standardEntries][3];
        boolean dataExists = false;
        if (SlideShow.standardImgMap != null) {
            for (int k = 0; k < SlideShow.standardImgMap.length; k++) {
                if (SlideShow.standardImgMap[k][2] == "1") {
                    dataExists = true;
                    break;
                }
            }
        }
        if (!dataExists) {
            Toast.makeText(getApplicationContext(), "Please run test first",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //maxV = getIntent().getIntArrayExtra("mMax");
        //minV = getIntent().getIntArrayExtra("mMin");
        //imgNames = getImgNames();
        //imgMaskAsHex = getImgMask();
        imgMaskAsHex = userPref.getString("hexMask", MainActivity.imgMaskAsString);
        if (SlideShow.isDefault) {
            standardImgMap = SlideShow.standardImgMap;
            imgMaskAsHex = "0x1";
        } else {
            standardImgMap = MainActivity.standardImgMap;
        }

        //Log.d(TAG, "getting string mask: " + maskAsBinaryString + " -> " + imgMaskAsHex);
        addCustom = userPref.getBoolean("custom", false);

        ActionBar actionBar = getActionBar();

        mMaxImV = SlideShow.mMaxIm;
        mMinImV = SlideShow.mMinIm;

        maxV = new int[MainActivity.gearsCount+1];
        minV = new int[MainActivity.gearsCount+1];
        if (SlideShow.standardImgMap != null) {
            checkData();
        }
        getTotalValues();
        if (mMaxImV != null) {
            if ((mMaxImV.length > MainActivity.standardEntries && addCustom)
            || (mMaxImV.length > MainActivity.standardEntries && MainActivity.currentUri != null)){
                imgMaskAsHex += "/cust";
            }
        }
            if (actionBar != null) {
                actionBar.setTitle(" Set '" + imgMaskAsHex + "'-" + MainActivity.product + "-" + MainActivity.barcode);
            }
        chartView.setArrays(minV, maxV);

        // int threshold = MainActivity.mDevice.diagFingerThreshold();
        // int satLevel =  MainActivity.mDevice.diagSaturationLevel();
        // int hysteresis = MainActivity.mDevice.diagFingerHysteresis();

        chartView.setThresholds(MainActivity.threshold, MainActivity.satLevel, MainActivity.hysteresis);
        touchInfo = (TextView) findViewById(R.id.infoText);
        String textToShowAtBottom = "Product: " + MainActivity.productInfo + "  Config: " + MainActivity.getTouchCfg();
        if (!MainActivity.panel.isEmpty()) {
            textToShowAtBottom += " Panel: " + MainActivity.panel;
        }
        touchInfo.setText(textToShowAtBottom);
        //chartView.invalidate();
    }

    public void checkData(){
        String[] tmp = new String[standardImgMap.length];
        needRetest = false;
        for (int i = 0; i < standardImgMap.length; i++) {
            if (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                            && SlideShow.standardImgMap[i][2].equals("1")) {
                Log.d(TAG, "Data for image " + i + " OK");
                tmp[i] = "0";
            } else if (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                    && SlideShow.standardImgMap[i][2].equals("0")) {
                Log.d(TAG, "No data for image " + i + " - need to run test again!");
                tmp[i] = "1";
                needRetest = true;
            }
        }
        int samples = Integer.parseInt(userPref.getString("samples", "100"));
        int gearsSet = userPref.getInt("gearsMask", 255);
        String imgUri = userPref.getString("uri", null);
        //boolean isModeAuto = userPref.getBoolean("isAuto", true);
        if (samples != SlideShow.samplesDone || gearsSet != SlideShow.gearsSetDone
                || (imgUri != null && !imgUri.equals(SlideShow.imgUriDone))) {
            needRetest = true;
        }
        if (needRetest) {

            //runFromChart = true;
            needRetest = false;

            AlertDialog.Builder builder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
            builder.setMessage(
                    "There is no enough test data for these settings!\n"
                            + "Click OK to run new test")
                    .setTitle("Oops...");

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getApplicationContext(),
                                    MainActivity.class);
                            startActivity(intent);
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
    }

    public static void getTotalValues(){
        for (int gear = 0; gear <= MainActivity.gearsCount; gear++) {
            tmpMax = 0;
            tmpMin = 0;
            for (int i = 0; i < SlideShow.CYCLES; i++) {
               if ((i >= standardImgMap.length && (addCustom || MainActivity.currentUri != null)) ||
                       (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                       && SlideShow.standardImgMap[i][2].equals("1"))) {
                   Log.d(TAG, "check data for image " + i);
                    if (tmpMax < mMaxImV[i][gear]) {
                        tmpMax = mMaxImV[i][gear];
                    }
                    if (tmpMin < mMinImV[i][gear]) {
                        tmpMin = mMinImV[i][gear];
                    }
                } else if (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                && SlideShow.standardImgMap[i][2].equals("0")) {
                    Log.d(TAG, "No data for image " + i + " - need to run test again!");
                }
            }
            maxV[gear] = tmpMax;
            minV[gear] = tmpMin;
            Log.d(TAG, "Getting total max/min for gear " + gear + ": " + maxV[gear] + "/" + minV[gear]);
        }
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chart_show, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SetPreferences.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(MainActivity.ABOUT_TITLE).setTitle(
                    MainActivity.ABOUT_VERSION);

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
    */

    public static String[] getImgNames() {
        Set<String> selections = userPref.getStringSet("advanced", null);
        String[] imgs = new String[selections.size()];
        imgs = selections.toArray(new String[]{});
        return imgs;
    }

    public static String getImgMask() {
        //String[] bitsToAdd = new String [MainActivity.valSet.length];
        maskAsBinaryString = "";
        String imgMaskAsString = "";

        boolean[] checked = new boolean[MainActivity.valSet.length];
        for (int i=0; i < MainActivity.valSet.length; i++) {
            checked[i] = false;
            for (int j=0; j < imgNames.length; j++) {
                if (MainActivity.valSet[i].equals(imgNames[j])) {
                    checked[i] = true;
                    continue;
                }
            }
            if (SlideShow.isDefault) {
                checked[0] = true;
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
        imgMaskAsString = "0x" + Integer.toString(decimal,16);
        Log.d(TAG, "try to save mask as hex: " + imgMask + " -> " + imgMaskAsString);
        return imgMaskAsString;
    }

    @Override
    public void onPause() {
        super.onPause();
        //MainActivity.currentUri = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //finish();

        Intent intent = new Intent(getApplicationContext(),
                MainActivity.class);
        startActivity(intent);
    }
}
