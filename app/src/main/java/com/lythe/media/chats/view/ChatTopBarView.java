package com.lythe.media.chats.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.lythe.media.R;

public class ChatTopBarView extends LinearLayout {
    private ImageButton btnBack;
    private ImageButton btnMore;
    private TextView tvContactName;
    private ImageView tvContactStatusTag;
    private TextView tvContactStatus;
    private OnChatTopBarActionListener listener;

    private final static String TAG = "ChatTopBarView";
    public interface OnChatTopBarActionListener {
        void onBackClick();
        void onMoreClick();
    }

    public ChatTopBarView(Context context) {
        super(context);
        initView(context);
    }

    public ChatTopBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ChatTopBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public ChatTopBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }
    public void setOnChatTopBarListener(OnChatTopBarActionListener listener) {
        Log.d(TAG, "setOnChatTopBarListener");
        this.listener = listener;
    }
    private void initView(Context context) {
        inflate(context, R.layout.layout_chat_top_bar, this);
        btnBack = findViewById(R.id.btn_back);
        btnMore = findViewById(R.id.btn_more);
        tvContactName = findViewById(R.id.tv_contact_name);
        tvContactStatusTag = findViewById(R.id.tv_contact_status_tag);
        tvContactStatus = findViewById(R.id.tv_contact_status);
        initEvents();
    }
    private void initEvents() {
        btnBack.setOnClickListener(v -> {
            if(listener != null) {
                listener.onBackClick();
            }
        });
        btnMore.setOnClickListener(v -> {
            if(listener != null) {
                listener.onMoreClick();
            }
        });
    }
    public void setTvContactName(String name) {
        this.tvContactName.setText(name);
    }
    public void setTvContactStatusTag(Integer id) {
//        shape_friend_online_status
//        shape_friend_unonline_status
        this.tvContactStatusTag.setImageResource(id);
    }
    public void setTvContactStatus(String status) {
        this.tvContactStatus.setText(status);
    }
}
