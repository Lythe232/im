package com.lythe.media.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.lythe.media.R;
import com.lythe.media.chats.viewmodel.FriendProfileViewModel;
import com.lythe.media.databinding.ActivityFriendProfileCardBinding;

public class FriendProfileCardActivity extends AppCompatActivity {
    ActivityFriendProfileCardBinding binding;
    FriendProfileViewModel viewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFriendProfileCardBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(FriendProfileViewModel.class);
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initView();
    }
    public void initView() {
        String id = getIntent().getStringExtra("id");
        String username = getIntent().getStringExtra("username");
        String avatar = getIntent().getStringExtra("avatar");
        String sign = getIntent().getStringExtra("sign");
        binding.tvNickname.setText(username);
        Glide.with(this)
                .load(TextUtils.isEmpty(avatar) ? R.drawable.avatar3 : avatar)
                .placeholder(R.drawable.avatar3)
                .error(R.drawable.avatar3)
                .transform(new CircleCrop())
                .into(binding.ivAvatar);
        binding.tvSignature.setText(sign);
        binding.tvUid.setText(id);
        binding.ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        viewModel.getFriendModelLiveData().observe(this, friendModel -> {
            binding.tvNickname.setText(friendModel.getName());
            binding.tvSignature.setText(friendModel.getSignature());
            Glide.with(this)
                    .load(TextUtils.isEmpty(friendModel.getAvatar())
                            ? R.drawable.avatar3 : friendModel.getAvatar())
                    .placeholder(R.drawable.avatar3)
                    .error(R.drawable.avatar3)
                    .transform(new CircleCrop())
                    .into(binding.ivAvatar);
            binding.tvUid.setText(friendModel.getId());
            binding.tvStatus.setText(friendModel.getStatus() == 0? "在线": "离线");
            binding.tvUid.setText(friendModel.getId());
        });

        viewModel.loadFriendProfile(id);
    }

}