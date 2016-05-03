package com.motorola.ghostbusters;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class CycleTestChartFragment extends Fragment {

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

    public static int mMaxImC[][][];
    public static int mMinImC[][][];

    public static int mMaxRxImC[][][];
    public static int mMinRxImC[][][];

    public static int mMaxTxImC[][][];
    public static int mMinTxImC[][][];

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

    MyChart chartView; //= (MyChart) rootView.findViewById(R.id.myChartC);
    MyAbsChart absChartView;
    TextView intTime;
    int currPage;

    @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.fragment_cycle_test_chart, container, false);
            MyChart chartView = (MyChart) rootView.findViewById(R.id.myChartC);
            MyAbsChart absChartView = (MyAbsChart) rootView.findViewById(R.id.myAbsChartC);
            TextView intTime = (TextView) rootView.findViewById(R.id.intTime);

        isRxEnabled = MainActivity.isRxEnabled;
        isTxEnabled = MainActivity.isTxEnabled;

        userPref = PreferenceManager
                .getDefaultSharedPreferences(this.getActivity());

        currPage = CycleTestChart.curr_Page;
        //String showIntTime = "intTime: " + MainActivity.intTime[currPage] + " ms";
        //intTime.setText(showIntTime);
/////////////
            testTypeToRun = MainActivity.testType;
            if (testTypeToRun.contains("2")) {
                chartView.setVisibility(View.VISIBLE);
                absChartView.setVisibility(View.INVISIBLE);
                intTime.setText("Int. Duration = " + SlideShow.intTime2[currPage]);
                } else if (testTypeToRun.contains("59")) {
                chartView.setVisibility(View.INVISIBLE);
                absChartView.setVisibility(View.VISIBLE);
                intTime.setText("Int. Duration = " + SlideShow.intTime59[currPage]);
            }

            imgMaskAsHex = userPref.getString("hexMask", MainActivity.imgMaskAsString);
            if (SlideShow.isDefault) {
                standardImgMap = SlideShow.standardImgMap;
                imgMaskAsHex = "0x1";
            } else {
                standardImgMap = MainActivity.standardImgMap;
            }

        boolean dataExists = false;
        if (SlideShow.standardImgMap != null && SlideShow.mMaxIm != null) {
            for (int k = 0; k < SlideShow.standardImgMap.length; k++) {
                if (SlideShow.standardImgMap[k][2] == "1") {
                    for (int l=0; l < SlideShow.mMaxIm[k].length; l++) {
                        if (MainActivity.mMaxImC[MainActivity.TEST_CYCLES-1][k][l] > 0) {
                            dataExists = true;
                            break;
                        }
                    }
                }
            }
        }
        SharedPreferences.Editor editor = userPref.edit();
        editor.putBoolean("report2_data_exists", dataExists);
        editor.commit();

        boolean absDataExists = false;
        if (SlideShow.standardImgMap != null && SlideShow.mRxMax != null) {
            for (int k = 0; k < SlideShow.standardImgMap.length; k++) {
                if (SlideShow.standardImgMap[k][2] == "1") {
                    for (int l=0; l < SlideShow.mRxMax[k].length; l++) {
                        if (MainActivity.mMaxRxImC[MainActivity.TEST_CYCLES-1][k][l] > 0) {
                            absDataExists = true;
                            break;
                        }
                    }
                }
            }
        }
        editor.putBoolean("report59_data_exists", absDataExists);
        editor.commit();

        Log.d(TAG, "dataExist: " + dataExists + "... absDataExist: " + absDataExists);

        if ((!dataExists && CycleTestChart.btnText.contains("2"))
                || (!absDataExists && CycleTestChart.btnText.contains("59"))) {
            Toast.makeText(getActivity(), "Please run test first",
                    Toast.LENGTH_SHORT).show();
        }


        //Log.d(TAG, "getting string mask: " + maskAsBinaryString + " -> " + imgMaskAsHex);
            addCustom = userPref.getBoolean("custom", false);
            //ActionBar actionBar = rootView.getActionBar();

            if (MainActivity.mMaxImC != null) {
                mMaxImV = MainActivity.mMaxImC[currPage];
                mMinImV = MainActivity.mMinImC[currPage];
            }

            if (MainActivity.mMaxRxImC != null) {
                mMaxRxImV = MainActivity.mMaxRxImC[currPage];
                mMinRxImV = MainActivity.mMinRxImC[currPage];
            }

            if (MainActivity.mMaxTxImC != null) {
                mMaxTxImV = MainActivity.mMaxTxImC[currPage];
                mMinTxImV = MainActivity.mMinTxImC[currPage];
            }

            maxV = new int[MainActivity.gearsCount+1];
            minV = new int[MainActivity.gearsCount+1];

            maxRxV = new int[MainActivity.stretches];
            minRxV = new int[MainActivity.stretches];

            maxTxV = new int[MainActivity.stretches];
            minTxV = new int[MainActivity.stretches];

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

            if (testTypeToRun.contains("59")) {
                absChartView.setArrays(minRxV, maxRxV, minTxV, maxTxV);
                absChartView.setThresholds(MainActivity.TxThreshold, MainActivity.RxThreshold);
                absChartView.setSwipeProgress(currPage);
            } else {
                chartView.setArrays(minV, maxV);
                chartView.setThresholds(MainActivity.threshold, MainActivity.satLevel, MainActivity.hysteresis);
                chartView.setSwipeProgress(currPage);
            }

/////////////
            return rootView;
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
            for (int gear = 0; gear < MainActivity.stretches; gear++) {
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
            for (int gear = 0; gear < MainActivity.stretches; gear++) {
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

}
