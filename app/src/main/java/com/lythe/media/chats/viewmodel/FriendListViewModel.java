package com.lythe.media.chats.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lythe.media.chats.data.entity.FriendConverter;
import com.lythe.media.chats.data.entity.FriendEntity;
import com.lythe.media.chats.data.entity.FriendListResponse;
import com.lythe.media.chats.data.model.FriendModel;
import com.lythe.media.chats.data.repository.FriendRepository;
import com.lythe.media.chats.data.repository.base.BaseRemoteRepository;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendListViewModel extends AndroidViewModel {
    private final static String TAG = "FriendListViewModel";
    private final MutableLiveData<List<FriendModel>> friendListItemMutableLiveData = new MutableLiveData<>();;

    public LiveData<List<FriendModel>> getFriendListItems() {
        return friendListItemMutableLiveData;
    }
    private FriendRepository friendRepository;
    private final ExecutorService processorExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(Throwable t);
    }
    public FriendListViewModel(@NonNull Application application) {
        super(application);
        friendRepository = FriendRepository.Companion.getInstance(application);
    }
    public void loadLocalFriends() {
        friendRepository.getLocalFriendList(new BaseRemoteRepository.Callback<List<FriendModel>>() {
            @Override
            public void onSuccess(List<FriendModel> data) {
                friendListItemMutableLiveData.postValue(data);
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }
    public void initLoad() {
        processorExecutor.execute(() -> {
            List<@NotNull FriendModel> localFriendsSync = friendRepository.getLocalFriendsSync();
            mainHandler.post(() -> friendListItemMutableLiveData.setValue(localFriendsSync));
        });
    }
    public void refreshFriends() {
        friendRepository.refreshFriends(new BaseRemoteRepository.Callback<List<FriendModel>>() {
            @Override
            public void onSuccess(List<FriendModel> data) {
                friendListItemMutableLiveData.postValue(data);
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, t.getMessage());
            }

        });
    }
}
