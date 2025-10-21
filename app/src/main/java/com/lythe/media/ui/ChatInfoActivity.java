package com.lythe.media.ui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lythe.media.PublicFragmentActivity;
import com.lythe.media.R;
import com.lythe.media.chats.data.entity.ConversationEntity;
import com.lythe.media.chats.data.entity.MessageConverter;
import com.lythe.media.chats.data.entity.MessageEntity;
import com.lythe.media.chats.utils.SendMessagesHelper;
import com.lythe.media.databinding.ActivityChatInfoBinding;
import com.lythe.media.chats.view.ChatInputView;
import com.lythe.media.chats.view.ChatTopBarView;
import com.lythe.media.chats.viewmodel.ChatInfoViewModel;
import com.lythe.media.im.MqttClientManager;
import com.lythe.media.im.messager.MessageBuildHelper;
import com.lythe.media.im.messager.queue.MessageQueue;
import com.lythe.media.protobuf.ImMessage;
import com.lythe.media.ui.adapter.ChatInfoMessageAdapter;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.lang.ref.WeakReference;

public class ChatInfoActivity extends AppCompatActivity {
    private final static String TAG = "ChatInfoActivity";
    private RecyclerView recyclerView;
    private ChatTopBarView chatTopBarView;
    private ChatInfoMessageAdapter adapter;
    private List<MessageEntity> messageEntities;
    private ChatInputView chatInputView;
    private ActivityChatInfoBinding binding_;
    private ChatInfoViewModel viewModel;
    private ConversationEntity conversationEntity;
    private ProgressBar loadingIndicator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        conversationEntity = (ConversationEntity) getIntent().getSerializableExtra("conversationEntity");
        binding_ = ActivityChatInfoBinding.inflate(getLayoutInflater());
        loadingIndicator = binding_.loadingIndicator;
        recyclerView = binding_.chatInfoRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        viewModel = new ViewModelProvider(this).get(ChatInfoViewModel.class);
        setContentView(binding_.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adapter = new ChatInfoMessageAdapter(new ArrayList<>());
        adapter.setOnAvatarClickListener(chatInfoMessageItem -> {
            Toast.makeText(this, chatInfoMessageItem.getContent(), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, FriendProfileCardActivity.class));

        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(!recyclerView.canScrollVertically(-1)) {
                    refreshMessages();
                }
            }
        });
        viewModel.loadMessagePaged(conversationEntity.getConversationId());

        final boolean[] isFirstLoad = {true};
        viewModel.getIsLoading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean) {
                    loadingIndicator.setVisibility(VISIBLE);
                } else {
                    loadingIndicator.setVisibility(GONE);
                }
            }
        });
        viewModel.getToastMsg().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(s != null) {
                    Toast.makeText(ChatInfoActivity.this, s, Toast.LENGTH_SHORT).show();
                }
            }
        });
        viewModel.getMessages().observe(this, new Observer<List<MessageEntity>>() {
            @Override
            public void onChanged(List<MessageEntity> messageEntities) {
                adapter.updateMessageInfo(messageEntities);

                if (isFirstLoad[0]) {
//                    recyclerView.post(() -> {
                        int lastPosition = messageEntities.size() - 1;
                        recyclerView.scrollToPosition(lastPosition);
//                    });
                    // 3. 首次加载完成后，标志位置为false，后续加载不再滚动
                    isFirstLoad[0] = false;
                }
            }
        });

        initChatInputViewEvents();
        initChatTopBarEvents();
    }

    private void initChatInputViewEvents() {
        chatInputView = binding_.chatInputView;
        chatInputView.setOnChatActionListener(new ChatInputView.OnChatActionListener() {
            @Override
            public void onSendText(String text) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                assert currentUser != null;
                Log.d(TAG, "Sending to " + conversationEntity.getConversationId());

                ImMessage imMessage = MessageBuildHelper
                        .buildTextMessageAuto(currentUser.getUid(),
                                conversationEntity.getConversationId(),
                                conversationEntity.getConversationId(),
                                text, true, 1024);

                SendMessagesHelper.getInstance().sendMessage(imMessage, conversationEntity.getConversationId(), 1);
                MessageEntity messageEntity = MessageConverter.INSTANCE.fromProto(imMessage, true);
                adapter.addMessageInfo(messageEntity);

                recyclerView.scrollToPosition(adapter.getItemCount() - 1);

            }

            @Override
            public void onVoiceRecordStart() {

            }

            @Override
            public void onVoiceRecordStop() {

            }

            @Override
            public void onEmojiClick() {

            }

            @Override
            public void onImageClick() {

            }

            @Override
            public void onFolderClick() {

            }
        });
    }
    private void initChatTopBarEvents() {
        chatTopBarView = binding_.chatTopBarView;
        chatTopBarView.setTvContactName(conversationEntity.getConversationName());
        // 使用WeakReference避免内存泄漏
        final WeakReference<ChatInfoActivity> weakActivity = new WeakReference<>(this);
        chatTopBarView.setOnChatTopBarListener(new ChatTopBarView.OnChatTopBarActionListener() {
            @Override
            public void onBackClick() {
                finish();
            }

            @Override
            public void onMoreClick() {
                ChatInfoActivity activity = weakActivity.get();
                if (activity == null || activity.isFinishing()) return;
                Intent intent = new Intent(activity, PublicFragmentActivity.class);
                intent.putExtra(PublicFragmentActivity.KEY_FRAGMENT_TYPE, PublicFragmentActivity.TYPE_CHAT_FRIEND_SETTING);
                startActivity(intent);
            }
        });
    }
    private void showMoreOptions(View view) {

    }

    public void refreshMessages() {
        viewModel.loadMessagePaged(conversationEntity.getConversationId());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理ViewBinding
        binding_ = null;

        // 移除监听器
        if (chatInputView != null) {
            chatInputView.setOnChatActionListener(null);
        }
        if (chatTopBarView != null) {
            chatTopBarView.setOnChatTopBarListener(null);
        }
        if (adapter != null) {
            adapter.setOnAvatarClickListener(null);
        }
        // 移除WindowInsets监听器
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), null);

        // 清理引用
        recyclerView = null;
        chatTopBarView = null;
        adapter = null;
        messageEntities = null;
        chatInputView = null;
        viewModel = null;
        conversationEntity = null;
    }
}