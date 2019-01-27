package com.motorola.ghostbusters;

import android.os.Build;
import android.util.Log;

import com.csvreader.CsvWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by elenalast on 3/19/17.
 */
public class ExportResults59 {
    private final static String TAG = "Ghostbusters";

    public static File resultsFile;
    public static File exportDir;

    public static int[] maxV;
    public static int[] minV;

    public static int[][] allMaxTxV;
    public static int[][] allMinTxV;
    public static int[][] allMaxRxV;
    public static int[][] allMinRxV;

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

    public static boolean isRxEnabled;
    public static boolean isTxEnabled;
    public static String HWrev = "";

    public static int exportResToCsv() {

        resultsFile = null;

        new File("sdcard" + File.separator + "Ghostbusters_results").mkdirs();
        exportDir = new File("sdcard" + File.separator + "Ghostbusters_results");

            if (report59ToFile()==1) {
                return 1;
            } else {
                return 0;
            }
    }

    public static int report59ToFile() {
        resultsFile = new File(exportDir.getAbsolutePath(), MainActivity.product + "-" + MainActivity.HWrev + "-" + Build.SERIAL +
                "-" + MainActivity.touchCfg + "-Report59.csv");


        try {
            if (resultsFile.exists()) {
                resultsFile.delete();
            }
            try {
                resultsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Export to csv is skipped due to file not exist");
                return 0;
            }
            Log.d(TAG, "File to export results is: " + resultsFile.getAbsolutePath());

            //Titles///////
            CsvWriter csvWrite = new CsvWriter(new FileWriter(resultsFile, true), ',');
            List<String> columnNames = new ArrayList<>();
            columnNames.add("product");
            columnNames.add("HW rev");
            columnNames.add("serial");
            columnNames.add("imgMask");
            columnNames.add("intDur");
            if (MainActivity.isRxEnabled) {
                columnNames.add("Rx-threshold.min");
                columnNames.add("Rx-threshold.max");
            }
            for (int i=0;i<MainActivity.stretches;i++) {
                if (MainActivity.isRxEnabled) {
                    columnNames.add("Rx-stretch" + i + ".min");
                    columnNames.add("Rx-stretch" + i + ".max");
                }
            }
            if (MainActivity.isTxEnabled) {
                columnNames.add("Tx-threshold.min");
                columnNames.add("Tx-threshold.max");
            }
            for (int i=0;i<MainActivity.stretches;i++) {
                if (MainActivity.isTxEnabled) {
                    columnNames.add("Tx-stretch" + i + ".min");
                    columnNames.add("Tx-stretch" + i + ".max");
                }
            }

            csvWrite.writeRecord(columnNames.toArray(new String[0]));
            //Records/////////////
            sortValuesRep59();
            List<String> valuesToWrite1 = new ArrayList<>();
            for (int j=0; j < MainActivity.TEST_CYCLES; j++) {
                valuesToWrite1.add(MainActivity.product);
                valuesToWrite1.add(MainActivity.HWrev);
                valuesToWrite1.add(MainActivity.barcode);
                valuesToWrite1.add(MainActivity.imgMaskAsString);
                valuesToWrite1.add(Integer.toString(MainActivity.intTime59[j]));
                if (MainActivity.isRxEnabled) {
                    valuesToWrite1.add(Integer.toString(-1 * MainActivity.RxThreshold));
                    valuesToWrite1.add(Integer.toString(MainActivity.RxThreshold));
                }

                for (int k=0; k < MainActivity.stretches; k++) {
                    if (MainActivity.isRxEnabled) {
                        valuesToWrite1.add(Integer.toString(-1 * allMinRxV[j][k]));
                        valuesToWrite1.add(Integer.toString(allMaxRxV[j][k]));
                    }
                }
                if (MainActivity.isTxEnabled) {
                    valuesToWrite1.add(Integer.toString(-1 * MainActivity.TxThreshold));
                    valuesToWrite1.add(Integer.toString(MainActivity.TxThreshold));
                }
                for (int k=0; k < MainActivity.stretches; k++) {
                    if (MainActivity.isTxEnabled) {
                        valuesToWrite1.add(Integer.toString(-1 * allMinTxV[j][k]));
                        valuesToWrite1.add(Integer.toString(allMaxTxV[j][k]));
                    }
                }
                csvWrite.writeRecord(valuesToWrite1.toArray(new String[0]));
                valuesToWrite1.clear();
            }

            //csvWrite.writeRecord(valuesToWrite1.toArray(new String[0]));
            //valuesToWrite1.clear();

            csvWrite.close();
        }

        catch (IOException e)   {
            Log.e("Export has failed... ", e.getMessage(), e);
            return 0;
        }

        return 1;

    }

    public static void sortValuesRep59() {

        if (SlideShow.isDefault) {
            standardImgMap = SlideShow.standardImgMap;
            //imgMaskAsHex = "0x1";
        } else {
            standardImgMap = MainActivity.standardImgMap;
        }

        allMaxTxV = new int[MainActivity.TEST_CYCLES][MainActivity.stretches];
        allMinTxV = new int[MainActivity.TEST_CYCLES][MainActivity.stretches];
        allMaxRxV = new int[MainActivity.TEST_CYCLES][MainActivity.stretches];
        allMinRxV = new int[MainActivity.TEST_CYCLES][MainActivity.stretches];

        for (int testCycle = 0; testCycle < MainActivity.TEST_CYCLES; testCycle++) {

            if (MainActivity.mMaxRxImC != null) {
                mMaxRxImV = MainActivity.mMaxRxImC[testCycle];
                mMinRxImV = MainActivity.mMinRxImC[testCycle];
            }
            if (MainActivity.mMaxTxImC != null) {
                mMaxTxImV = MainActivity.mMaxTxImC[testCycle];
                mMinTxImV = MainActivity.mMinTxImC[testCycle];
            }

            maxTxV = new int[MainActivity.stretches];
            minTxV = new int[MainActivity.stretches];
            maxRxV = new int[MainActivity.stretches];
            minRxV = new int[MainActivity.stretches];

            if (MainActivity.isRxEnabled) {
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
                allMaxRxV[testCycle] = maxRxV;
                allMinRxV[testCycle] = minRxV;
            }
            if (MainActivity.isTxEnabled) {
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
                allMaxTxV[testCycle] = maxTxV;
                allMinTxV[testCycle] = minTxV;
            }
        }
    }
}
