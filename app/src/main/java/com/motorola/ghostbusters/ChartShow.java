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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class ChartShow extends Activity {
    MyChart chartView;
    MyAbsChart absChartView;
    public static final String TAG = "Ghostbusters";
    public static SharedPreferences userPref;
    public static int[] maxV;
    public static int[] minV;

    public static int[] maxRxV;
    public static int[] minRxV;

    public static int[] maxTxV;
    public static int[] minTxV;

    public static int mMaxImV[][];
    public static int mMinImV[][];

    public static int mMaxRxImV[][];
    public static int mMinRxImV[][];

    public static int mMaxTxImV[][];
    public static int mMinTxImV[][];

    private static int tmpMax;
    private static int tmpMin;

    public static String[] imgNames;
    public static String imgMaskAsHex = "";
    public static String maskAsBinaryString;
    public static String[][] standardImgMap;
    public static boolean addCustom;
    public static boolean needRetest;
    TextView touchInfo;
    String testTypeToRun;
    public static boolean isRxEnabled;
    public static boolean isTxEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_show);
        chartView = (MyChart) findViewById(R.id.myChart);
        absChartView = (MyAbsChart) findViewById(R.id.myAbsChart);

        userPref = PreferenceManager
                .getDefaultSharedPreferences(this);
        standardImgMap = new String[MainActivity.standardEntries][3];

        if (MainActivity.testType.contains("59") && MainActivity.mDevice.diagHasHybridBaselineControl() != 1) {
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
        } else {
            isRxEnabled = MainActivity.isRxEnabled;
            isTxEnabled = MainActivity.isTxEnabled;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.d(TAG, "call onResume");

        testTypeToRun = MainActivity.testType;

        Button switchButton = (Button) findViewById(R.id.switchType);

        if (testTypeToRun.contains("2")) {
            chartView.setVisibility(View.VISIBLE);
            absChartView.setVisibility(View.INVISIBLE);
            switchButton.setText("Report 2 // Switch report type");
        } else if (testTypeToRun.contains("59")) {
            chartView.setVisibility(View.INVISIBLE);
            absChartView.setVisibility(View.VISIBLE);
            switchButton.setText("Report 59 // Switch report type");
        }

        boolean dataExists = false;
        if (SlideShow.standardImgMap != null && SlideShow.mMaxIm != null) {
            for (int k = 0; k < SlideShow.standardImgMap.length; k++) {
                if (SlideShow.standardImgMap[k][2] == "1") {
                    for (int l=0; l < SlideShow.mMaxIm[k].length; l++) {
                        if (SlideShow.mMaxIm[k][l] > 0) {
                            dataExists = true;
                            break;
                        }
                    }
                }
            }
        }
        boolean absDataExists = false;
        if (SlideShow.standardImgMap != null && SlideShow.mRxMax != null) {
            for (int k = 0; k < SlideShow.standardImgMap.length; k++) {
                    if (SlideShow.standardImgMap[k][2] == "1") {
                        for (int l=0; l < SlideShow.mRxMax[k].length; l++) {
                            if (SlideShow.mRxMax[k][l] > 0) {
                                absDataExists = true;
                                break;
                            }
                        }
                    }
            }
        }
        Log.d(TAG, "dataExist: " + dataExists + "... absDataExist: " + absDataExists);
        if ((!dataExists && switchButton.getText().toString().contains("2"))
                || (!absDataExists && switchButton.getText().toString().contains("59"))) {
            Toast.makeText(getApplicationContext(), "Please run test first",
                    Toast.LENGTH_SHORT).show();
        }

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

        mMaxRxImV = SlideShow.mRxMax;
        mMinRxImV = SlideShow.mRxMin;

        mMaxTxImV = SlideShow.mTxMax;
        mMinTxImV = SlideShow.mTxMin;

        maxV = new int[MainActivity.gearsCount+1];
        minV = new int[MainActivity.gearsCount+1];

        maxRxV = new int[MainActivity.gearsCount+1];
        minRxV = new int[MainActivity.gearsCount+1];

        maxTxV = new int[MainActivity.gearsCount+1];
        minTxV = new int[MainActivity.gearsCount+1];

        if (SlideShow.standardImgMap != null) {
            checkData();
        }
        getTotalValues();
        getAbsData();

        if (mMaxImV != null) {
            if ((mMaxImV.length > MainActivity.standardEntries && addCustom)
                    || (mMaxImV.length > MainActivity.standardEntries && MainActivity.currentUri != null)){
                imgMaskAsHex += "/cust";
            }
        }

        if (mMaxRxImV != null && !imgMaskAsHex.contains("/cust")) {
            if ((mMaxRxImV.length > MainActivity.standardEntries && addCustom)
                    || (mMaxRxImV.length > MainActivity.standardEntries && MainActivity.currentUri != null)){
                imgMaskAsHex += "/cust";
            }
        }

        if (mMaxTxImV != null && !imgMaskAsHex.contains("/cust")) {
            if ((mMaxTxImV.length > MainActivity.standardEntries && addCustom)
                    || (mMaxTxImV.length > MainActivity.standardEntries && MainActivity.currentUri != null)){
                imgMaskAsHex += "/cust";
            }
        }

        if (actionBar != null) {
            actionBar.setTitle(" Set '" + imgMaskAsHex + "'-" + MainActivity.product + "-" + MainActivity.barcode);
            //actionBar.
        }

        if (testTypeToRun.contains("59")) {
            absChartView.setArrays(minRxV, maxRxV, minTxV, maxTxV);
            absChartView.setThresholds(MainActivity.TxThreshold, MainActivity.RxThreshold);
        } else {
            chartView.setArrays(minV, maxV);
            chartView.setThresholds(MainActivity.threshold, MainActivity.satLevel, MainActivity.hysteresis);
        }

        touchInfo = (TextView) findViewById(R.id.infoText);
        String textToShowAtBottom = "Product: " + MainActivity.productInfo + "  Config: " + MainActivity.getTouchCfg();
        if (!MainActivity.panel.isEmpty()) {
            textToShowAtBottom += " Panel: " + MainActivity.panel;
        }
        touchInfo.setText(textToShowAtBottom);

    }

    public void checkData(){
        String[] tmp = new String[standardImgMap.length];
        needRetest = false;
        for (int i = 0; i < standardImgMap.length; i++) {
            if (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                            && SlideShow.standardImgMap[i][2].equals("1")) {
                //Log.d(TAG, "Data for image " + i + " OK");
                tmp[i] = "0";
            } else if (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                    && SlideShow.standardImgMap[i][2].equals("0")) {
                Log.d(TAG, "No data for image " + i + " - need to run test again!");
                tmp[i] = "1";
                needRetest = true;
            }
        }
        int samples = Integer.parseInt(userPref.getString("samples", "100"));
        //int gearsSet = userPref.getInt("gearsMask", 255);
        String imgUri = userPref.getString("uri", null);
        //boolean isModeAuto = userPref.getBoolean("isAuto", true);
        if (samples != SlideShow.samplesDone ||
                (imgUri != null && !imgUri.equals(SlideShow.imgUriDone))) {
            needRetest = true;
        }
        if (needRetest) {

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
                   //Log.d(TAG, "check data for image " + i);
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

    public static void getAbsData() {
        if (isRxEnabled) {
            for (int gear = 0; gear <= MainActivity.gearsCount; gear++) {
                tmpMax = 0;
                tmpMin = 0;
                for (int i = 0; i < SlideShow.CYCLES; i++) {
                    if ((i >= standardImgMap.length && (addCustom || MainActivity.currentUri != null)) ||
                            (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                                    && SlideShow.standardImgMap[i][2].equals("1"))) {
                        //Log.d(TAG, "check data for Rx for image " + i);
                        if (tmpMax < mMaxRxImV[i][gear]) {
                            tmpMax = mMaxRxImV[i][gear];
                        }
                        if (tmpMin < mMinRxImV[i][gear]) {
                            tmpMin = mMinRxImV[i][gear];
                        }
                    } else if (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                            && SlideShow.standardImgMap[i][2].equals("0")) {
                        Log.d(TAG, "No data for Rx for image " + i + " - need to run test again!");
                    }
                }
                maxRxV[gear] = tmpMax;
                minRxV[gear] = tmpMin;
                Log.d(TAG, "Getting total Rx max/min for gear " + gear + ": " + maxRxV[gear] + "/" + minRxV[gear]);
            }
        }
        if (isTxEnabled) {
            for (int gear = 0; gear <= MainActivity.gearsCount; gear++) {
                tmpMax = 0;
                tmpMin = 0;
                for (int i = 0; i < SlideShow.CYCLES; i++) {
                    if ((i >= standardImgMap.length && (addCustom || MainActivity.currentUri != null)) ||
                            (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                                    && SlideShow.standardImgMap[i][2].equals("1"))) {
                        //Log.d(TAG, "check data for Tx for image " + i);
                        if (tmpMax < mMaxTxImV[i][gear]) {
                            tmpMax = mMaxTxImV[i][gear];
                        }
                        if (tmpMin < mMinTxImV[i][gear]) {
                            tmpMin = mMinTxImV[i][gear];
                        }
                    } else if (i < standardImgMap.length && standardImgMap[i][1].equals("1")
                            && SlideShow.standardImgMap[i][2].equals("0")) {
                        Log.d(TAG, "No data for Tx for image " + i + " - need to run test again!");
                    }
                }
                maxTxV[gear] = tmpMax;
                minTxV[gear] = tmpMin;
                Log.d(TAG, "Getting total Tx max/min for gear " + gear + ": " + maxTxV[gear] + "/" + minTxV[gear]);
            }
        }
        }

    public void switchChart(View view) {

        AlertDialog.Builder b = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_CustDialog)));
        b.setTitle("Select report type");
        String[] types = {"Report 2", "Report 59"};
        b.setItems(types, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {
                    case 0:
                        MainActivity.testType = "Report 2";
                        break;
                    case 1:
                        MainActivity.testType = "Report 59";
                        break;
                }
                dialog.dismiss();
            }

        });
        b.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
                startActivity(getIntent());
            }
        });

        b.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(),
                MainActivity.class);
        startActivity(intent);
    }
}
