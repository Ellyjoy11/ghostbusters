package com.motorola.ghostbusters;

/**
 * Created by elenalast on 4/15/17.
 */
import android.content.Context;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.StorageObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class CloudStorage {

    private static final String TAG = "Ghostbusters";
    public static Storage storage;
    public static Context mContext;
    public static File tmpFileP12;

    public static void uploadFile(File p12file, String bucketName, String filePath) throws Exception {

        //mContext = context;
        tmpFileP12 = p12file;
        storage = getStorage();
        StorageObject object = new StorageObject();
        object.setBucket(bucketName);
        File file = new File(filePath);
        Log.d(TAG, "file path is " + filePath);

        InputStream stream = new FileInputStream(file);

        try {
            String contentType = URLConnection.guessContentTypeFromStream(stream);
            InputStreamContent content = new InputStreamContent(contentType,stream);

            Storage.Objects.Insert insert = storage.objects().insert(bucketName, null, content);
            insert.setName(file.getName());
            insert.execute();

        } finally {
            stream.close();
        }
    }

    private static Storage getStorage() throws Exception {
        Log.d(TAG, "I am here");
        if (storage == null) {
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = new JacksonFactory();
            List<String> scopes = new ArrayList<String>();
            scopes.add(StorageScopes.DEVSTORAGE_FULL_CONTROL);

            Credential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(jsonFactory)
                    .setServiceAccountId("38707967618-compute@developer.gserviceaccount.com") //Email
                    .setServiceAccountPrivateKeyFromP12File(tmpFileP12)
                    .setServiceAccountScopes(scopes).build();

            storage = new Storage.Builder(httpTransport, jsonFactory,
                    credential).setApplicationName("GCSUpload")
                    .build();
        }

        return storage;
    }
}