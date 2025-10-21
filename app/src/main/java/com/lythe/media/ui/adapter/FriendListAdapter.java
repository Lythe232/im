package com.lythe.media.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.lythe.media.R;
import com.lythe.media.chats.data.entity.FriendConverter;
import com.lythe.media.chats.data.model.FriendListItem;
import com.lythe.media.chats.data.model.FriendModel;

import org.w3c.dom.Text;

import java.util.List;

public class FriendListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
//    private List<FriendModel> friendModelList;
    private final static String TAG = "FriendListAdapter";
    private List<FriendListItem> friendListItems;
    private final Context context;
    OnItemClickListener listener;
    public FriendListAdapter(Context context, List<FriendListItem> friendListItems) {
        this.context = context;
        this.friendListItems = friendListItems;
    }

    @Override
    public int getItemViewType(int position) {
//        return super.getItemViewType(position);
        switch (friendListItems.get(position).getItemType()) {
            case LETTER_TITLE -> {
                return 0;
            }
            case FRIEND_ITEM -> {
                return 1;
            }
            default ->  {
                return -1;
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if(viewType == 0) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_letter_title, parent, false);
            return new LetterTitleViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
            return new FriendViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FriendListItem item = friendListItems.get(position);
        if(holder instanceof LetterTitleViewHolder letterHolder) {
            item.getFriend().getLetter();
            letterHolder.letterTv.setText(item.getFriend().getLetter());
        } else if (holder instanceof FriendViewHolder friendHolder) {
            if(item.getFriend() != null) {
                FriendModel friend = item.getFriend();
                String name = friend.getUsername();
                friendHolder.nameTextView.setText((name == null || name.isEmpty()) ? "ta什么都没说": name);
                friendHolder.statusTextView.setText(FriendConverter.INSTANCE.status2Text(friend.getStatus()));
                friendHolder.statusImageView.setImageResource(FriendConverter.INSTANCE.status2Image(friend.getStatus()));
                friendHolder.signTextView.setText(friend.getSignature());
                // 加载头像
                String avatar = item.getFriend().getAvatar();
                if(avatar != null && !avatar.isEmpty()) {
                    Glide.with(context)
                            .load(avatar)
                            .placeholder(R.drawable.avatar3)
                            .error(R.drawable.avatar3)
                            .into(friendHolder.avatarImageView);
                } else {
                    friendHolder.avatarImageView.setImageResource(R.drawable.avatar3);
                }
            }
            friendHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null) {
                        listener.onclick(item, v);
                    } else {
                        Log.d(TAG, "Not impl");
                    }
                }
            });
        }
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    @Override
    public int getItemCount() {
        return friendListItems.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        ImageView statusImageView;
        TextView statusTextView;
        TextView signTextView;
        ShapeableImageView avatarImageView;
        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatar);
            nameTextView = itemView.findViewById(R.id.friend_name);
            statusImageView = itemView.findViewById(R.id.friend_status);
            statusTextView = itemView.findViewById(R.id.friend_status_text);
            signTextView = itemView.findViewById(R.id.friend_sign);
            itemView.setBackgroundResource(R.drawable.selector_click_feedback);
            itemView.setClickable(true);
            itemView.setFocusable(true);
        }
    }

    public static class LetterTitleViewHolder extends RecyclerView.ViewHolder {
        TextView letterTv;
        public LetterTitleViewHolder(@NonNull View itemView) {
            super(itemView);
            letterTv = itemView.findViewById(R.id.tv_letter);
            itemView.setBackgroundResource(R.drawable.selector_click_feedback);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<FriendListItem> newList) {
        this.friendListItems = newList;
        notifyDataSetChanged();
    }
    public List<FriendListItem> getItems() {
        return this.friendListItems;
    }
    public interface OnItemClickListener {
        void onclick(FriendListItem item, View v);
    }
}
