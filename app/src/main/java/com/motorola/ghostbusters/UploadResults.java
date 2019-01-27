package com.motorola.ghostbusters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class UploadResults {

    private final static String TAG = "Ghostbusters";
    private static ArrayList<String> csvList;
    public static Context mContext;
    public static ArrayList selectedItems;
    public static File resultsDir = new File("sdcard" + File.separator + "Ghostbusters_results");
    public static ArrayList<File> fList;
    public static File tmpFile;

    public static void uploadFunction(Context context) {
        mContext = context;
        csvList = new ArrayList<String>();
        final CharSequence[] items;
        //setContentView(R.layout.activity_upload_results);
        ////
        if (checkConnectivity(mContext)) {
                csvList = findFilesToUpload();
                if (!csvList.isEmpty()) {
                    items = new CharSequence[csvList.size()];
                    selectedItems=new ArrayList();
                    fList = new ArrayList<File>();
                    String message = "";
                    for (int i=0; i<csvList.size(); i++) {
                        message += csvList.get(i) + "\n";
                        items[i] = csvList.get(i);
                    }

                    AlertDialog dialog = new AlertDialog.Builder(mContext)
                            .setTitle("Select Files to Upload")
                            .setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                                    if (isChecked) {
                                        // If the user checked the item, add it to the selected items
                                        selectedItems.add(indexSelected);
                                    } else if (selectedItems.contains(indexSelected)) {
                                        // Else, if the item is already in the array, remove it
                                        selectedItems.remove(Integer.valueOf(indexSelected));
                                    }
                                }
                            }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Log.d(TAG, "number of files selected: " + selectedItems.size());
                                    for (int j=0; j < items.length; j++) {
                                        if (selectedItems.contains(j)) {
                                            tmpFile = new File(resultsDir, csvList.get(j));
                                            fList.add(tmpFile);
                                        }
                                    }
                                    UploadResults inst = new UploadResults();
                                    inst.uploadFilesInBackground();
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            }).create();
                    dialog.show();

                } else {
                    //Show dialog about nothing to upload
                    Log.d(TAG, "nothing to upload");
                }

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mContext, R.style.Theme_CustDialog));

            builder.setMessage("You have no internet connection.\nPlease try again once you connected to internet").setTitle(
                    "Oops");

            builder.setPositiveButton("OK",
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

    protected static boolean checkConnectivity(Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
            return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
        }

    protected static ArrayList<String> findFilesToUpload() {
        //TODO
        ArrayList<String> namesList = new ArrayList<String>();

        if (!resultsDir.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mContext, R.style.Theme_CustDialog));

            builder.setMessage("You have nothing to upload yet!").setTitle(
                    "Oops");

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            File[] list = resultsDir.listFiles();

            for (File f : list) {
                if (f.getAbsolutePath().contains(".csv")) {
                    namesList.add(f.getName());
                }
            }
            }

        return namesList;
    }

    public void uploadFilesInBackground() {
        //Log.d(TAG, "files number: " + fList.size());
        uploadFileInBackground[] uplTasks = new uploadFileInBackground[fList.size()];
        for (int k=0; k < fList.size(); k++) {
            uplTasks[k] = new uploadFileInBackground(mContext, fList.get(k).getAbsolutePath());
            Log.d(TAG, "file is added: " + fList.get(k));
            uplTasks[k].execute();
        }

    }


    private File getTempPkc12File() throws IOException {
        // xxx.p12 export from google API console
        Resources res = mContext.getResources();
        InputStream pkc12Stream = res.openRawResource(R.raw.my_key_0673c2d409e9);
        File tempPkc12File = File.createTempFile("temp_pkc12_file", "p12");
        OutputStream tempFileStream = new FileOutputStream(tempPkc12File);

        int read = 0;
        byte[] bytes = new byte[1024];
        while ((read = pkc12Stream.read(bytes)) != -1) {
            Log.d("TEST_APP_DEBUG", "reading key file");
            tempFileStream.write(bytes, 0, read);
        }
        return tempPkc12File;
    }

    private class uploadFileInBackground extends AsyncTask<Integer, Integer, Integer> {
        //////////
        private Context context;
        //private Integer result;
        String filePath = "";

        public uploadFileInBackground(Context context, String fileName) {
            this.context = context;
            filePath = fileName;
        }

        protected Integer doInBackground(Integer... params) {
            try {
                Log.d("TEST_APP_DEBUG", "Trying to start upload");
                CloudStorage.uploadFile(getTempPkc12File(), "gb_test_bucket", filePath);

            } catch (Exception e) {
                Log.d("TEST_APP_DEBUG", "Exception: " + e.getMessage());
                e.printStackTrace();
                //result = 0;
                return 0;
            }
            //result = 0;
            return 1;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 1) {
                Toast.makeText(mContext, "Upload is done", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Error!!!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
