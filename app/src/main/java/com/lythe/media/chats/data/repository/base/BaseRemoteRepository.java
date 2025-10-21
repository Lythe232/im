package com.lythe.media.chats.data.repository.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public abstract class BaseRemoteRepository {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(Throwable t);
    }

    protected<T> void execute(Call<T> call, Callback<T> callback) {
        executor.execute(() -> {
            try {
                Response<T> response = call.execute();
                if(response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("请求失败，code=" + response.code()));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
}
