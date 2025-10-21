package com.lythe.media.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.lythe.media.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding_;
    private TextInputEditText usernameEditText_;
    private TextInputEditText emailEditText_;
    private TextInputEditText passwordEditText_;
    private TextInputEditText confirmPasswordEditText_;
    private MaterialButton registerButton_;
    private TextView loginLink_;
    private FirebaseAuth auth_;
    LoginViewModel viewModel;
    private final static String TAG = "RegisterActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RegisterActivity that = this;
        auth_ = FirebaseAuth.getInstance();
        binding_ = ActivityRegisterBinding.inflate(getLayoutInflater());
        usernameEditText_ = binding_.usernameEditText;
        emailEditText_ = binding_.emailEditText;
        passwordEditText_ = binding_.passwordEditText;
        confirmPasswordEditText_ = binding_.confirmPasswordEditText;
        registerButton_ = binding_.registerButton;
        loginLink_ = binding_.loginLink;
        viewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);
        EdgeToEdge.enable(this);
        setContentView(binding_.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding_.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        viewModel.getRegisterResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(LoginResult loginResult) {
                if(loginResult == null) {
                    return;
                }
                if(loginResult.getSuccess() != null) {
                    showToast("注册成功，请重新登录");
                    finish();
                    startActivity(new Intent(that, LoginRegisterActivity.class));
                }
                if(loginResult.getError() != null) {
                    showToast("注册失败");
                }
            }
        });
        registerButton_.setOnClickListener(view -> {
            if(!validateForm()) {
//                Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
            } else {
                viewModel.createAccount(emailEditText_.getText().toString().trim(),
                        passwordEditText_.getText().toString().trim());
            }
        });
        loginLink_.setOnClickListener(view -> {
            startActivity(new Intent(this, LoginRegisterActivity.class));
            finish();
        });
    }

    private boolean validateForm() {
        Log.d(TAG, "validateForm");
        boolean valid = true;
        String username = usernameEditText_.getText().toString().trim();
        String email = emailEditText_.getText().toString().trim();
        String password = passwordEditText_.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText_.getText().toString().trim();

        if(TextUtils.isEmpty(username)) {
            usernameEditText_.setError("Required.");
            valid = false;
        } else {
            usernameEditText_.setError(null);
        }
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
        if(TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText_.setError("Required.");
            valid = false;
        } else {
            confirmPasswordEditText_.setError(null);
        }
        if(!password.equals(confirmPassword)) {
            Log.d(TAG, "password not equal.");
            passwordEditText_.setError("Not equal.");
            confirmPasswordEditText_.setError("Not equal.");
            valid = false;
        }
        return valid;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding_ = null;
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}