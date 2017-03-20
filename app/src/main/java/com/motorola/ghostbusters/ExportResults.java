package com.motorola.ghostbusters;

import android.os.Build;
import android.util.Log;

import com.csvreader.CsvWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by elenalast on 3/19/17.
 */
public class ExportResults {
    private final static String TAG = "Ghostbusters";

    public static int exportResToCsv() {

        File file = null;

        new File("sdcard" + File.separator + "Ghostbusters_results").mkdirs();
        File exportDir = new File("sdcard" + File.separator + "Ghostbusters_results");
        file = new File(exportDir.getAbsolutePath(), MainActivity.product + "-" + Build.SERIAL + "-Ghostbusters.csv");


        try {
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Export to csv is skipped due to file not exist");
                return 0;
            }
            Log.d(TAG, "File to export results is: " + file.getAbsolutePath());

            CsvWriter csvWrite = new CsvWriter(new FileWriter(file, true), ',');
            String[] columnNames = {"product", "serial", "report", "threshold"};

            csvWrite.writeRecord(columnNames);
            String[] nextRec = {MainActivity.product, MainActivity.barcode, MainActivity.testType,
                    Integer.toString(MainActivity.threshold)};
            csvWrite.writeRecord(nextRec);
        }

        catch (IOException e)   {
            Log.e("Export has failed... ", e.getMessage(), e);
        }
        return 1;
    }

}
