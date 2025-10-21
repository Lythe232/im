package com.lythe.media;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lythe.media.databinding.ActivityMainBinding;
import com.lythe.media.im.MqttClientManager;
import com.lythe.media.im.utils.MqttServiceManager;
import com.lythe.media.ui.FriendFragment;
import com.lythe.media.ui.ChatFragment;
import com.lythe.media.ui.FeedFragment;
import com.lythe.media.ui.login.LoginRegisterActivity;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding_;
    private BottomNavigationView bottomNavigationView_;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private LinearLayout homeBar;
    private View navHeader;
//    private FirebaseAuth.AuthStateListener authStateListener;


    private final static String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding_ = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding_.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding_.drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        bottomNavigationView_ = binding_.bottomNavigation;
        bottomNavigationView_.setOnItemReselectedListener(navListener);

//
        ChatFragment chatFragment = new ChatFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(binding_.container.getId(), chatFragment)
                .commit();
        initView();
        initEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initView() {
        homeBar = binding_.homeBar;
        navigationView = binding_.navView;
        navHeader = navigationView.getHeaderView(0);
        drawerLayout = binding_.drawerLayout;

    }
    private void initEvents() {
        MainActivity that = this;
//        authStateListener = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//                if(currentUser != null) {
//
//                } else {
//                    finish();
//                    startActivity(new Intent(that, LoginRegisterActivity.class));
//                    showToast("退出登录");
//                }
//            }
//        };
        navigationView.setNavigationItemSelectedListener(item -> {
            if(item.getItemId() == R.id.sidebar_item_logout) {
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(that, LoginRegisterActivity.class));
                MqttServiceManager.getInstance(getApplicationContext()).onUserLogout();
            }
            drawerLayout.closeDrawer(navigationView);
            return true;
        });
        binding_.homeBarMenu.setOnClickListener(v -> {
            navigateToMenu();
        });
        navHeader.setOnClickListener(item -> {
            showToast("点击了Header");
        });
    }
    private NavigationBarView.OnItemReselectedListener navListener = new NavigationBarView.OnItemReselectedListener() {
        @Override
        public void onNavigationItemReselected(@NonNull MenuItem item) {
            Log.d(TAG, "Reselected(");
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if(itemId == R.id.nav_home) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.nav_friend) {
                selectedFragment = new FriendFragment();
            } else if (itemId == R.id.nav_post) {
                selectedFragment = new FeedFragment();
            } else {
                selectedFragment = new ChatFragment();
            }
                getSupportFragmentManager().beginTransaction()
                        .replace(binding_.container.getId(), selectedFragment)
                        .commit();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(authStateListener != null) {
//            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
//        }
        binding_ = null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void navigateToMenu() {
        drawerLayout.openDrawer(binding_.navView);
    }
}