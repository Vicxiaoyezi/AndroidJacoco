package com.jacoco.android.util;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import okhttp3.*;

public class ClientUploadUtils {

    private static String tempPath;
    private static final String url = "http://localhost:8080/file/upload";

    static private void uploadFile(File file) throws Exception {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("files", file.getName(), RequestBody.create(file, MediaType.get("application/plain")))
                .addFormDataPart("originalPath", file.getParent().replace(tempPath, ""))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        response.body();
    }

    static private void uploadDir(File file) throws Exception {
        for (File files : Objects.requireNonNull(file.listFiles())){
            if(files.isDirectory())
                uploadDir(files);
            else {
                uploadFile(files);
            }
        }
    }

    static public void upload(String dirPath) throws Exception {
        ClientUploadUtils.tempPath = new File(dirPath).getParent();

        File file = new File(dirPath);
        if(file.isDirectory())
            uploadDir(file);
        else {
            uploadFile(file);
        }
    }

    public static void main(String[] args) throws Exception {
        upload("/Users/vic/Jacoco/AndroidJacoco/app/build/outputs/report");
    }
}
