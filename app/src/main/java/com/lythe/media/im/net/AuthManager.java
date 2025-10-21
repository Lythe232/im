package com.lythe.media.im.net;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AuthManager implements FirebaseAuth.AuthStateListener {
    private static final String TAG = "AuthManager";
    private static  volatile AuthManager instance;
    private String cachedToken;
    private String cachedUid;
    private Long expiryTime;
    private boolean isRefreshing = false;
    private final CountDownLatch refreshLatch = new CountDownLatch(1);

    public static AuthManager getInstance() {
        Log.d(TAG, "AuthManager getInstance");
        if(instance == null) {
            synchronized (AuthManager.class) {
                if(instance == null) {
                    instance = new AuthManager();
                }
            }
        }
        return instance;
    }
    private AuthManager() {
        FirebaseAuth.getInstance().addAuthStateListener(this);
    }

    public AuthInfo getAuthInfo() {
        return new AuthInfo(cachedToken, cachedUid);
    }

    public void updateAuthInfo(String token, String uid, Long expiryTime) {
        this.cachedToken = token;
        this.cachedUid = uid;
        this.expiryTime = expiryTime;
    }

    public void clearAuthInfo() {
        this.cachedToken = null;
        this.cachedUid = null;
        this.expiryTime = null;
    }
    public boolean isTokenValid() {
        return cachedToken != null && System.currentTimeMillis() < this.expiryTime;
    }
    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null) {

        } else {

        }
    }

    public void refreshTokenAsync(TokenRefreshCallback callback) {
        Log.d(TAG, "refreshTokenAsync");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser  == null) {
            if(callback != null)
                callback.onTokenRefreshed(false);
            return;
        }
        if(isRefreshing) {
            Log.d(TAG, "Token is refreshing");

            if(callback != null) {
                callback.onTokenRefreshed(false);
            }
            return;
        }
        Log.d(TAG, "Refresh token");
        isRefreshing = true;
        currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    isRefreshing = false;
                    if(task.isSuccessful()) {
                        GetTokenResult result = task.getResult();
                        String token = result.getToken();
                        String uid = currentUser.getUid();

                        long expiryTime = System.currentTimeMillis() +
                                (result.getExpirationTimestamp() -
                                        result.getAuthTimestamp()) * 1000 - 300000;
                        updateAuthInfo(token, uid, expiryTime);
                        Log.d(TAG, "Token refreshed - UID: " + uid);
                        if(callback != null) {
                            callback.onTokenRefreshed(true);
                        }
                    } else {
                        Log.e(TAG, "Failed to refresh token", task.getException());
                        if(callback != null) {
                            callback.onTokenRefreshed(false);
                        }
                    }
                    refreshLatch.countDown();
                });
    }

    public boolean refreshTokenSync() {
        Log.d(TAG, "refreshTokenSync");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            return false;
        }

        if(isRefreshing) {
            Log.d(TAG, "RefreshTokenSync isRefreshing");
            try {
                refreshLatch.await(10, TimeUnit.SECONDS);
                return isTokenValid();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        refreshLatch.countDown();
        final boolean[] success = new boolean[1];
        CountDownLatch latch = new CountDownLatch(1);

        refreshTokenAsync(new TokenRefreshCallback() {
            @Override
            public void onTokenRefreshed(boolean result) {
                success[0] = result;
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
            return success[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void ensureAuthInfo(TokenRefreshCallback callback) {
        Log.d(TAG, "ensureAuthInfo");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if(currentUser != null && (!isTokenValid() || isRefreshing)) {
            refreshTokenAsync(callback);
        } else  {
            callback.onTokenRefreshed(isTokenValid());
        }
    }


    public interface TokenRefreshCallback {
        void onTokenRefreshed(boolean success);
    }
    public class AuthInfo {
        private final String token;
        private final String uid;
        public AuthInfo(String token, String uid) {
            this.token = token;
            this.uid = uid;
        }

        public String getToken() {
            return token;
        }

        public String getUid() {
            return uid;
        }
        public boolean hasAuthInfo() {
            return token != null && uid != null;
        }
    }
}
