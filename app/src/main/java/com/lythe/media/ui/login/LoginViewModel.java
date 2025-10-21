package com.lythe.media.ui.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Log;
import android.util.Patterns;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.lythe.media.R;
import com.lythe.media.chats.utils.MqttMessageSender;
import com.lythe.media.chats.utils.OkHttpFileUploader;
import com.lythe.media.im.MqttClientManager;
import com.lythe.media.im.utils.MqttServiceManager;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class LoginViewModel extends ViewModel {
    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private MutableLiveData<LoginResult> registerResult = new MutableLiveData<>();
    private FirebaseAuth auth_ = FirebaseAuth.getInstance();
    private final String TAG = "LoginViewModel";

    public LoginViewModel() {
    }
    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    LiveData<LoginResult> getRegisterResult() {
        return registerResult;
    }
    public void login(String username, String password) {
        Log.i(TAG, "signInWithEmailAndPassword");
        auth_.signInWithEmailAndPassword(username, password).addOnCompleteListener(
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.i(TAG, "signInWithEmailAndPassword complete");

                        if(task.isSuccessful()) {
                            Log.i(TAG, "signInWithEmailAndPassword complete successful");
                            onAuthSuccess();
                            FirebaseUser currentUser = auth_.getCurrentUser();
                            assert currentUser != null;
                            currentUser.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                @Override
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    Log.d(TAG, "_______TOKEN__________");
                                    Log.d(TAG, task.getResult().getToken());
                                    Log.d(TAG, "_________________");
                                }
                            });

                            loginResult.postValue(new LoginResult(new LoggedInUserView(currentUser.getDisplayName())));
                            Log.i(TAG, "currentUser");

                        } else {
                            Log.i(TAG, "signInWithEmailAndPassword complete failed");
                            Exception exception = task.getException();
                            if(exception instanceof FirebaseAuthInvalidCredentialsException || exception instanceof FirebaseAuthInvalidUserException) {
                                loginResult.setValue(new LoginResult(R.string.invalid_username_password));
                                Log.e("FirebaseLogin", "Username or Password Invalid: " + exception.getMessage());
                            } else  {
                                loginResult.setValue(new LoginResult(R.string.login_failed));
                                Log.e("FirebaseLogin", "Authentication error: " + exception.getMessage());
                            }
                        }
                    }
                }
        );
    }
    public void signOut() {
        if(auth_ != null) {
            auth_.signOut();
        }
    }
    public void createAccount(String email, String password) {
        auth_.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            registerResult.postValue(new LoginResult(new LoggedInUserView("newaccount")));
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            registerResult.postValue(new LoginResult(R.string.register_failed));
                        }
                    }
                });
    }
    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
    private void onAuthSuccess() {

    }
}