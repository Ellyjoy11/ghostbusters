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
public class ExportResults {
    private final static String TAG = "Ghostbusters";

    public static File resultsFile;
    public static File exportDir;

    public static int[] maxV;
    public static int[] minV;

    public static int[][] allMaxV;
    public static int[][] allMinV;

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

    public static int exportResToCsv() {

        resultsFile = null;

        new File("sdcard" + File.separator + "Ghostbusters_results").mkdirs();
        exportDir = new File("sdcard" + File.separator + "Ghostbusters_results");

        if (MainActivity.testType.contains("2")) {
            if (report2ToFile()==1) {
                return 1;
            } else {
                return 0;
            }
        }

        return 0;
    }

    public static int report2ToFile() {
        resultsFile = new File(exportDir.getAbsolutePath(), MainActivity.product + "-" + Build.SERIAL + "-Ghostbusters-Report2.csv");


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
            columnNames.add("serial");
            columnNames.add("imgMask");
            columnNames.add("min/max");
            columnNames.add("intDur");
            for (int i=0;i<MainActivity.gearsCount;i++) {
                columnNames.add("gear" + i);
            }
            columnNames.add("auto");

            csvWrite.writeRecord(columnNames.toArray(new String[0]));
            //Records/////////////
            sortValuesRep2();
            List<String> valuesToWrite1 = new ArrayList<>();
            List<String> valuesToWrite2 = new ArrayList<>();
            for (int j=0; j < MainActivity.TEST_CYCLES; j++) {
                valuesToWrite1.add(MainActivity.product);
                valuesToWrite1.add(MainActivity.barcode);
                valuesToWrite1.add(MainActivity.imgMaskAsString);
                valuesToWrite1.add("min");
                valuesToWrite1.add(Integer.toString(MainActivity.intTime2[j]));
                valuesToWrite2.add(" ");
                valuesToWrite2.add(" ");
                valuesToWrite2.add(" ");
                valuesToWrite2.add("max");
                valuesToWrite2.add(" ");
                for (int k=0; k < MainActivity.gearsCount+1; k++) {
                    valuesToWrite1.add(Integer.toString(-1*allMinV[j][k]));
                    valuesToWrite2.add(Integer.toString(allMaxV[j][k]));
                }
                csvWrite.writeRecord(valuesToWrite1.toArray(new String[0]));
                csvWrite.writeRecord(valuesToWrite2.toArray(new String[0]));
                valuesToWrite1.clear();
                valuesToWrite2.clear();
            }
            valuesToWrite1.add(" ");
            valuesToWrite1.add(" ");
            valuesToWrite1.add("");
            valuesToWrite1.add("");
            valuesToWrite1.add("minThreshold");
            valuesToWrite2.add(" ");
            valuesToWrite2.add(" ");
            valuesToWrite2.add(" ");
            valuesToWrite2.add(" ");
            valuesToWrite2.add("maxThreshold");
            for (int k=0; k < MainActivity.gearsCount+1; k++) {
                if (k==0 || k==MainActivity.gearsCount) {
                    valuesToWrite1.add(Integer.toString(-1*MainActivity.threshold));
                    valuesToWrite2.add(Integer.toString(MainActivity.threshold));
                } else {
                    valuesToWrite1.add(" ");
                    valuesToWrite2.add(" ");
                }
            }
            csvWrite.writeRecord(valuesToWrite1.toArray(new String[0]));
            csvWrite.writeRecord(valuesToWrite2.toArray(new String[0]));
            valuesToWrite1.clear();
            valuesToWrite2.clear();

            csvWrite.close();
        }

        catch (IOException e)   {
            Log.e("Export has failed... ", e.getMessage(), e);
            return 0;
        }

        return 1;

    }

    public static void sortValuesRep2() {

        if (SlideShow.isDefault) {
            standardImgMap = SlideShow.standardImgMap;
            //imgMaskAsHex = "0x1";
        } else {
            standardImgMap = MainActivity.standardImgMap;
        }

        allMaxV = new int[MainActivity.TEST_CYCLES][MainActivity.gearsCount + 1];
        allMinV = new int[MainActivity.TEST_CYCLES][MainActivity.gearsCount + 1];

        for (int testCycle=0; testCycle < MainActivity.TEST_CYCLES; testCycle++) {

            if (MainActivity.mMaxImC != null) {
                mMaxImV = MainActivity.mMaxImC[testCycle];
                mMinImV = MainActivity.mMinImC[testCycle];
            }

            maxV = new int[MainActivity.gearsCount + 1];
            minV = new int[MainActivity.gearsCount + 1];

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
            allMaxV[testCycle] = maxV;
            allMinV[testCycle] = minV;
        }
    }

}
