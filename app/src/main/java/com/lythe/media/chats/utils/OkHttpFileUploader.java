package com.lythe.media.chats.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.lythe.media.protobuf.ImMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class OkHttpFileUploader implements SendMessagesHelper.FileUploader {
    private static final String TAG = "OkHttpFileUploader";
    private OkHttpClient okHttpClient;
    private Call currentCall;
    private OnFileUploadProgressListener currentProgressListener;
    public OkHttpFileUploader() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String uploadFile(ImMessage request, OnFileUploadProgressListener onFileUploadProgressListener) throws Exception {

        currentProgressListener = currentProgressListener;

//        File file = new File(request.getFileLocalPath());
//        if(!file.exists()) {
//            throw new FileNotFoundException("文件不存在: " + request.getFileLocalPath());
//        }
//
//
//        MultipartBody requestBody = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("file", file.getName(),
//                        new ProgressRequestBody(file, request.getFileMimeType(), onFileUploadProgressListener))
//                .addFormDataPart("dialogId", String.valueOf(request.getDialogId()))
//                .build();
//        Request okRequest = new Request.Builder()
//                .url("")
//                .post(requestBody)
//                .build();
//        currentCall = okHttpClient.newCall(okRequest);
//        Response response = null;
//        try {
//            response = currentCall.execute();
//            if(!response.isSuccessful()) {
//                String errorMsg = "上传请求失败，状态码：" + response.code();
//                Log.e(TAG, errorMsg);
//                throw new IOException("上传失败: " + response.code());
//            }
//            String responseBody = response.body().string();
//            JSONObject json = new JSONObject(responseBody);
//            if(json.getBoolean("success")) {
//                String fileUrl = json.getString("fileUrl");
//                Log.d(TAG, "文件上传成功，服务器URL：" + fileUrl);
//                return fileUrl;
//            } else {
//                String errorMsg = "上传失败，服务器提示：" + json.getString("msg");
//                Log.e(TAG, errorMsg);
//                throw new Exception(errorMsg);
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "上传过程异常", e);
//            clearCurrentTaskResources();
//            throw e;
//        } finally {
//            clearCurrentTaskResources();
//            if (response != null && response.body() != null) {
//                response.body().close();
//            }
//        }
        return "";
    }

    @Override
    public void cancelUpload() {
        if(currentCall != null && currentCall.isCanceled()) {
            currentCall.cancel();
            if(currentProgressListener != null) {
                currentProgressListener.onCanceled();
                Log.d(TAG, "已通知进度回调：上传已取消");
            }
        }
        clearCurrentTaskResources();
    }
    private void clearCurrentTaskResources() {
        currentCall = null;
        currentProgressListener = null;
    }
    private static class ProgressRequestBody extends RequestBody {
        private final File file;
        private final String mimeType;
        private final OnFileUploadProgressListener listener;

        public ProgressRequestBody(File file, String mimeType, OnFileUploadProgressListener listener) {
            this.file = file;
            this.mimeType = mimeType;
            this.listener = listener;
        }

        @Nullable
        @Override
        public MediaType contentType() {
            return MediaType.parse(mimeType);
        }

        @Override
        public long contentLength() {
            return file.length();
        }

        @Override
        public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {
            long length = file.length();
            byte[] buffer = new byte[4096];
            FileInputStream fileInputStream = new FileInputStream(file);
            long uploaded = 0;

            try {
                int read;
                while ((read = fileInputStream.read(buffer)) != -1) {
                    if(Thread.currentThread().isInterrupted()) {
                        throw new IOException("上传已取消");
                    }

                    uploaded += read;
                    bufferedSink.write(buffer, 0, read);
                    int process = (int) (uploaded * 100 / length);
                    if(listener != null) {
                        listener.onProgress(process, uploaded, length);
                    }
                }
            } finally {
                fileInputStream.close();
            }

        }
    }
}
