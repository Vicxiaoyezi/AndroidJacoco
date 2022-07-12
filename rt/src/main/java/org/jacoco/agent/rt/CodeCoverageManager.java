package org.jacoco.agent.rt;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class CodeCoverageManager {
    private static CodeCoverageManager sInstance=new CodeCoverageManager();
    private static String URL_HOST = "http://10.0.0.99:8080";//内网 服务器地址

    private static String APP_NAME;
    private static int versionCode;

    private static String dirPath;
    private static String filePath;

    private static final String TAG = "CodeCoverageManager";


    public static void init(Context context,String serverHost){
       String path=context.getFilesDir().getAbsolutePath();
        APP_NAME=context.getPackageName().replace(".","");
        versionCode=getVersion(context);
        if(serverHost!=null)
            URL_HOST=serverHost;

        dirPath = path + "/jacoco/" + versionCode + "/";
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();
        filePath = dirPath + UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + ".ec";

        File f = new File(filePath);
        Log.d(TAG, filePath + " canRead=" + f.canRead() + " canWrite=" + f.canWrite());

    }

    public static void generateCoverageFile() {
        sInstance.writeToFile();
    }

    public static void uploadData() {
        sInstance.upload();
    }

    private static int getVersion(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private CodeCoverageManager() {
    }

    /**
     * 生成executionData
     */
    private void writeToFile() {

        if(filePath==null) return;
        OutputStream out = null;
        try {
            out = new FileOutputStream(filePath, false);
            IAgent agent = RT.getAgent();
            out.write(agent.getExecutionData(false));
            Log.i(TAG, " generateCoverageFile write success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, " generateCoverageFile Exception:" + e.toString());
        } finally {
            close(out);
        }
    }

    private void close(Closeable out) {
        if(out!=null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void upload(){
        if(filePath==null) return;

        new Thread(){
            @Override
            public void run() {
                super.run();

                Log.d(TAG,"开始上传 "+Thread.currentThread());
                try {
                    syncUploadFiles();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"uploadData "+e.getMessage());
                }
                Log.d(TAG,"执行完成");
            }
        }.start();
    }


    private void syncUploadFiles() throws IOException {
        OkHttpClient client = new OkHttpClient();
        File dir = new File(dirPath);
        if (dir.exists() && dir.list().length > 0) {

            Log.d(TAG,"File list="+ Arrays.toString(dir.list()));
            File[] files=dir.listFiles();
            for (File file : files) {
                if (!file.getName().endsWith(".ec") || file.length()<=0) continue;
                RequestBody requestBody = new MultipartBody.Builder()
                        .addFormDataPart("files", file.getName(), RequestBody.create(file, MediaType.get("application/plain")))
                        .addFormDataPart("originalPath", "/" + APP_NAME + "/" + versionCode)
                        .build();

                Request request = new Request.Builder()
                        .url(URL_HOST + "/upload")
                        .post(requestBody)
                        .build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String str = response.body().string();
                    Log.d(TAG, " success =" + str);
                    if(str.contains("200")){
                        file.delete();
                    }
                } else {
                    Log.e(TAG, " error =" + response.code());
                }
            }
        }

    }
}
