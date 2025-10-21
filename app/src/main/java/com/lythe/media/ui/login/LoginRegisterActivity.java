package com.lythe.media.ui.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lythe.media.MainActivity;
import com.lythe.media.R;
import com.lythe.media.databinding.ActivityLoginRegisterBinding;
import com.lythe.media.im.utils.MqttServiceManager;

public class LoginRegisterActivity extends AppCompatActivity {
    private ActivityLoginRegisterBinding binding_;
    private TextInputEditText emailEditText_, passwordEditText_;
    private MaterialButton loginButton_;
    private TextView registerText_;
    private FirebaseAuth auth_;

    private LoginViewModel loginViewModel_;
    private final static String TAG = "LoginRegisterActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoginRegisterActivity that = this;
        auth_ = FirebaseAuth.getInstance();
        binding_ = ActivityLoginRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding_.getRoot());
        emailEditText_ = binding_.emailEditText;
        passwordEditText_ = binding_.passwordEditText;
        loginButton_ = binding_.loginButton;
        registerText_ = binding_.registerText;
        loginViewModel_ = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        loginButton_.setEnabled(false);
        loginViewModel_.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(LoginFormState loginFormState) {
                if(loginFormState == null) {
                    return;
                }
                loginButton_.setEnabled(loginFormState.isDataValid());
                if(loginFormState.getUsernameError() != null) {
                    emailEditText_.setError(getString(loginFormState.getUsernameError()));
                }
                if(loginFormState.getPasswordError() != null) {
                    passwordEditText_.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });
        loginViewModel_.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
//                loadingProgressBar.setVisibility(View.GONE);

                if (loginResult.getError() != null) {
                    if(FirebaseAuth.getInstance().getCurrentUser() != null) {
                        FirebaseAuth.getInstance().signOut();
                        MqttServiceManager.getInstance(getApplicationContext()).onUserLogout();
                    }
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {

                    updateUiWithUser(loginResult.getSuccess());
                    setResult(Activity.RESULT_OK);
                    //Complete and destroy login activity once successful
                    MqttServiceManager.getInstance(getApplicationContext()).onUserLogin();
                    finish();
                    startActivity(new Intent(that, MainActivity.class));
                }

            }
        });
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel_.loginDataChanged(emailEditText_.getText().toString(), passwordEditText_.getText().toString());
            }
        };
        emailEditText_.addTextChangedListener(textWatcher);
        passwordEditText_.addTextChangedListener(textWatcher);
        passwordEditText_.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel_.login(emailEditText_.getText().toString(),
                            passwordEditText_.getText().toString());
                }
                return false;
            }
        });
        loginButton_.setOnClickListener(view -> {
            String email = emailEditText_.getText().toString().trim();
            String password = passwordEditText_.getText().toString().trim();
            signIn(email, password);
        });
        registerText_.setOnClickListener(view -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth_.getCurrentUser();
        if(currentUser != null) {
            reload();
        }
    }

    private void signIn(String email, String password) {
        if(!validateForm()) {
            return ;
        }
        Log.i(TAG, "signIn");
        loginViewModel_.login(email, password);
    }
    private void signOut() {
        Log.i(TAG, "signOut");
        loginViewModel_.signOut();
    }
    private void reload() {
        LoginRegisterActivity that = this;
        auth_.getCurrentUser().reload().addOnCompleteListener(new OnCompleteListener<Void>() {

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(that,
                            "Reload successful!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "reload", task.getException());
                    Toast.makeText(that,
                            "Failed to reload user.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
    private boolean validateForm() {
        Log.d(TAG, "validateForm");
        boolean valid = true;
        String email = emailEditText_.getText().toString().trim();
        String password = passwordEditText_.getText().toString().trim();
        if(TextUtils.isEmpty(email)) {
            emailEditText_.setError("Required.");
            valid = false;
        } else {
            emailEditText_.setError(null);
        }
        if(TextUtils.isEmpty(password)) {
            passwordEditText_.setError("Required.");
            valid = false;
        } else {
            passwordEditText_.setError(null);
        }
        return valid;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding_ = null;
    }
}