package com.lythe.media.ui.adapter;

import static android.view.View.GONE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.lythe.media.R;
import com.lythe.media.chats.data.entity.MessageEntity;

import java.util.Collections;
import java.util.List;

public class ChatInfoMessageAdapter extends RecyclerView.Adapter<ChatInfoMessageAdapter.MessageViewHolder> {
    public List<MessageEntity> messageList;
    private OnAvatarClickListener onAvatarClickListener;

    public ChatInfoMessageAdapter(List<MessageEntity> chatInfoMessageItemList) {
        this.messageList = chatInfoMessageItemList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_info_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageEntity message = messageList.get(position);
        holder.textView.setText(message.getContent());
        holder.textViewByMe.setText(message.getContent());
        boolean isSentByMe = message.isSelf();

        holder.leftMessageLinearLayout.setVisibility(isSentByMe ? GONE : View.VISIBLE);
        holder.rightMessageLinearLayout.setVisibility(isSentByMe ? View.VISIBLE : GONE);
        // 设置头像点击事件
        View.OnClickListener avatarClickListener = v -> {
            if (onAvatarClickListener != null) {
                onAvatarClickListener.onItemClick(message);
            }
        };

        holder.avatar.setOnClickListener(avatarClickListener);
        holder.avatarRight.setOnClickListener(avatarClickListener);
        holder.progressBar.setVisibility(GONE);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textView, textViewByMe;
        ShapeableImageView avatar,avatarRight;
        LinearLayout leftMessageLinearLayout, rightMessageLinearLayout;
        ProgressBar progressBar;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.chat_info_message_text);
            textViewByMe = itemView.findViewById(R.id.right_chat_info_message_text);
            avatar = itemView.findViewById(R.id.chat_info_message_avatar);
            avatarRight = itemView.findViewById(R.id.chat_info_message_avatar_right);
            leftMessageLinearLayout= itemView.findViewById(R.id.left_chat_info_message);
            rightMessageLinearLayout= itemView.findViewById(R.id.right_chat_info_message);
            progressBar = itemView.findViewById(R.id.right_message_loading);
        }
    }
    public void updateMessageInfo(List<MessageEntity> messageList) {
        Collections.reverse(messageList);
        this.messageList = messageList;

        notifyDataSetChanged();
    }
    public void addMessageInfo(MessageEntity message) {
        this.messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    public void setOnAvatarClickListener(OnAvatarClickListener listener) {
        this.onAvatarClickListener = listener;
    }

    public interface OnAvatarClickListener {
        void onItemClick(MessageEntity messageEntity);
    }
}
