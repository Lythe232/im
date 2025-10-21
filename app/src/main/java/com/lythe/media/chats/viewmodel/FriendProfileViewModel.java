package com.lythe.media.chats.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lythe.media.chats.data.model.FriendModel;
import com.lythe.media.chats.data.repository.FriendRepository;
import com.lythe.media.chats.data.repository.base.BaseRemoteRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FriendProfileViewModel extends AndroidViewModel {
    private final static String TAG = "FriendProfileViewModel";
    private MutableLiveData<FriendModel> friendModelMutableLiveData = new MutableLiveData<FriendModel>();
    private FriendRepository friendRepository;
    private ExecutorService processorExecutor =  Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public FriendProfileViewModel(@NonNull Application application) {
        super(application);
        friendRepository = FriendRepository.Companion.getInstance(application);
    }
    public LiveData<FriendModel> getFriendModelLiveData() {
        return friendModelMutableLiveData;
    }
    public void loadFriendProfile(String uid) {
        processorExecutor.execute(() -> {
            friendRepository.loadFriendProfile(uid, new BaseRemoteRepository.Callback<FriendModel>() {
                @Override
                public void onSuccess(FriendModel data) {
                    friendModelMutableLiveData.postValue(data);
                }

                @Override
                public void onError(Throwable t) {
                    Log.d(TAG, t.getMessage());
                }
            });
        });

    }

    @Override
    protected void onCleared() {
        super.onCleared();
        shutdownExecutor();
    }
    private void shutdownExecutor() {
        if(!processorExecutor.isShutdown()) {
            processorExecutor.shutdown();
            try {
                if (!processorExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    processorExecutor.shutdownNow(); // 强制立即关闭
                }
            } catch (InterruptedException e) {
                processorExecutor.shutdownNow();
            }
        }
    }
}
