package com.motorola.ghostbusters;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CycleTestChart extends FragmentActivity {

    private static int NUM_PAGES = MainActivity.TEST_CYCLES;
    public static ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    public static int curr_Page;

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

    public static String btnText="switch report type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_test_chart);
        /////////////////////////////////////////
        // Instantiate a ViewPager and a PagerAdapter.

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new CycleTestChartAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //curr_Page = mPager.getCurrentItem();
                //mPagerAdapter.notifyDataSetChanged();

            }

            @Override
            public void onPageSelected(int position) {

                curr_Page = mPager.getCurrentItem();
                mPagerAdapter.notifyDataSetChanged();
                //mPager.destroyDrawingCache();
                //mPager.refreshDrawableState();

                //Log.d(TAG, "int time must be shown: " + MainActivity.intTime[mPager.getCurrentItem()]);

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //curr_Page = mPager.getCurrentItem();
                //mPagerAdapter.notifyDataSetChanged();

            }
        });

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
                                    SetPreferences.class);
                            startActivity(intent);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            isRxEnabled = MainActivity.isRxEnabled;
            isTxEnabled = MainActivity.isTxEnabled;
        }

        mMaxImV = SlideShow.mMaxIm;
        mMinImV = SlideShow.mMinIm;

        mMaxRxImV = SlideShow.mRxMax;
        mMinRxImV = SlideShow.mRxMin;

        mMaxTxImV = SlideShow.mTxMax;
        mMinTxImV = SlideShow.mTxMin;

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

        imgMaskAsHex = userPref.getString("hexMask", MainActivity.imgMaskAsString);
        if (SlideShow.isDefault) {
            standardImgMap = SlideShow.standardImgMap;
            imgMaskAsHex = "0x1";
        } else {
            standardImgMap = MainActivity.standardImgMap;
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(" Set '" + imgMaskAsHex + "'-" + MainActivity.product + "-" + MainActivity.barcode);
        }

        String textToShowAtBottom = "Product: " + MainActivity.productInfo + "  Config: " + MainActivity.getTouchCfg();
        if (!MainActivity.panel.isEmpty()) {
            textToShowAtBottom += " Panel: " + MainActivity.panel;
        }
        touchInfo = (TextView) findViewById(R.id.infoTextCC);
        touchInfo.setText(textToShowAtBottom);

    }

    @Override
    public void onResume() {
        super.onResume();
        NUM_PAGES = MainActivity.TEST_CYCLES;
        curr_Page = mPager.getCurrentItem();
        mPagerAdapter.notifyDataSetChanged();


        Button switchButton = (Button) findViewById(R.id.switchTypeCC);

        testTypeToRun = MainActivity.testType;
        if (testTypeToRun.contains("2")) {
            btnText = "Report 2 // switch report type";
        } else if (testTypeToRun.contains("59")) {
            btnText = "Report 59 // switch report type";
        }
        switchButton.setText(btnText);



        //Log.d(TAG, "call onResume");

        //testTypeToRun = MainActivity.testType;

        /*
        chartView = (MyChart) mPager.getRootView().findViewById(R.id.myChartC);
        absChartView = (MyAbsChart) mPager.getRootView().findViewById(R.id.myAbsChartC);
        Button switchButton = (Button) mPager.getRootView().findViewById(R.id.switchTypeC);
        touchInfo = (TextView) mPager.getRootView().findViewById(R.id.infoTextC);

        if (testTypeToRun.contains("2")) {
            chartView.setVisibility(View.VISIBLE);
            absChartView.setVisibility(View.INVISIBLE);
            switchButton.setText("Report 2 // Switch report type");
        } else if (testTypeToRun.contains("59")) {
            chartView.setVisibility(View.INVISIBLE);
            absChartView.setVisibility(View.VISIBLE);
            switchButton.setText("Report 59 // Switch report type");
        }
 */
/*
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

        if ((!dataExists && btnText.contains("2"))
                || (!absDataExists && btnText.contains("59"))) {
            Toast.makeText(getApplicationContext(), "Please run test first",
                    Toast.LENGTH_SHORT).show();
        }
*/

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

    //////////////////
    @Override
    public void onBackPressed() {
        //if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
            Intent intent = new Intent(getApplicationContext(),
                    MainActivity.class);
            startActivity(intent);
        //} else {
            // Otherwise, select the previous step.
         //   mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        //}
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class CycleTestChartAdapter extends FragmentStatePagerAdapter {
        public CycleTestChartAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new CycleTestChartFragment();
        }

        @Override
        public int getItemPosition(Object object) {

            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
    //////////////
}
