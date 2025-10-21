package com.lythe.media.ui.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lythe.media.R;
import com.lythe.media.chats.data.entity.ConversationEntity;
import com.lythe.media.chats.utils.TimeUtils;
import com.lythe.media.chats.view.UnreadBadgeView;

import java.util.Date;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private List<ConversationEntity> chatList;
    private OnItemClickListener onItemClickListener;

    public ChatListAdapter(List<ConversationEntity> chatList) {
        this.chatList = chatList;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        public TextView name, lastMessage, timestamp;
        public UnreadBadgeView unreadBadgeView;
        public ImageView avatar;
        public View rootView;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.chat_item_name);
            lastMessage = itemView.findViewById(R.id.chat_item_last_message);
            timestamp = itemView.findViewById(R.id.chat_item_timestamp);
            avatar = itemView.findViewById(R.id.chat_item_avatar);
            unreadBadgeView = itemView.findViewById(R.id.chat_item_unread);
            rootView = itemView;

            timestamp.setSingleLine(true);
        }
    }
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ConversationEntity chatItem = chatList.get(position);
        holder.name.setText(chatItem.getConversationName());
        holder.lastMessage.setText(chatItem.getLastMessage());
        holder.timestamp.setText(TimeUtils.formatChatTime(chatItem.getLastMessageTime()));
        holder.unreadBadgeView.setUnreadCount(chatItem.getUnreadCount());
        holder.rootView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(chatItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    public interface OnItemClickListener {
        void onItemClick(ConversationEntity chatItem);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateChatList(List<ConversationEntity> chatList) {
        this.chatList.clear();
        this.chatList.addAll(chatList);
        notifyDataSetChanged();
    }
}
