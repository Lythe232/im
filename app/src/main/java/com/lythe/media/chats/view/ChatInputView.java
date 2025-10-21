package com.lythe.media.chats.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.lythe.media.R;

public class ChatInputView extends LinearLayout {

    public interface OnChatActionListener {
        void onSendText(String text);// 发送文字消息
        void onVoiceRecordStart();   // 开始录音
        void onVoiceRecordStop();    // 停止录音
        void onEmojiClick();         // 表情按钮点击
        void onImageClick();         // 图片按钮点击
        void onFolderClick();          // 文件按钮点击
    }
    private OnChatActionListener listener;
    private EditText etInput;
    private Button btnSend;
    private Button btnHoldToTalk;
    private ImageButton btnVoice;
    private ImageButton btnEmoji;
    private ImageButton btnImage;
    private ImageButton btnFolder;
    private TextView tvVoiceHint;
    private boolean isVoiceMode = false;


    public ChatInputView(Context context) {
        super(context);
        initView(context);
    }

    public ChatInputView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);

    }

    public ChatInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);

    }

    public ChatInputView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.layout_chat_input, this);
        // 绑定控件
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        btnHoldToTalk = findViewById(R.id.btn_hold_to_talk);
        btnVoice = findViewById(R.id.btn_voice);
        btnEmoji = findViewById(R.id.btn_emoji);
        btnImage = findViewById(R.id.btn_image);
        btnFolder = findViewById(R.id.btn_folder);
        tvVoiceHint = findViewById(R.id.tv_voice_hint);
        initEvents();
    }
    @SuppressLint("ClickableViewAccessibility")
    private void initEvents() {
// 语音模式切换
        btnVoice.setOnClickListener(v -> toggleVoiceMode());

        // 发送按钮点击
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!TextUtils.isEmpty(text) && listener != null) {
                listener.onSendText(text);
                etInput.setText(""); // 清空输入框
            }
        });

        // 表情按钮点击
        btnEmoji.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmojiClick();
            }
        });

        // 图片按钮点击
        btnImage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick();
            }
        });

        // 文件按钮点击
        btnFolder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFolderClick();
            }
        });

        // 输入框文本变化监听（控制发送按钮状态）
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSend.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 按住说话逻辑
        btnHoldToTalk.setOnTouchListener((v, event) -> {
            if (listener == null) return true;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 开始录音
                    listener.onVoiceRecordStart();
                    btnHoldToTalk.setText("松开发送");
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // 停止录音
                    listener.onVoiceRecordStop();
                    btnHoldToTalk.setText("按住说话");
                    break;
            }
            return true;
        });
    }
    // 切换语音/文字模式
    private void toggleVoiceMode() {
        isVoiceMode = !isVoiceMode;
        if (isVoiceMode) {
            // 语音模式
            etInput.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
            btnHoldToTalk.setVisibility(View.VISIBLE);
        } else {
            // 文字模式
            etInput.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.VISIBLE);
            btnHoldToTalk.setVisibility(View.GONE);
        }
    }

    // 设置回调监听器
    public void setOnChatActionListener(OnChatActionListener listener) {
        this.listener = listener;
    }

    // 外部调用：清空输入框
    public void clearInput() {
        etInput.setText("");
    }

    // 外部调用：获取输入的文本
    public String getInputText() {
        return etInput.getText().toString().trim();
    }
}
