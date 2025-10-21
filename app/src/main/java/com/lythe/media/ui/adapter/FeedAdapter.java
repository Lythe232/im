package com.lythe.media.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lythe.media.R;
import com.lythe.media.ui.chats.data.model.CommentModel;
import com.lythe.media.ui.chats.data.model.FeedModel;
import com.lythe.media.ui.chats.data.model.UserModel;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedItemViewHolder> {

    private List<FeedModel> feedModels;
    private Context context;
    private OnFeedActionListener onFeedActionListener;
    public FeedAdapter(Context context, List<FeedModel> feedModels, OnFeedActionListener onFeedActionListener) {
        this.context = context;
        this.feedModels = feedModels;
        this.onFeedActionListener = onFeedActionListener;
    }
    public interface OnFeedActionListener {
        void onLikeClick(FeedModel feed);
        void onCommentClick(FeedModel feed);
        void onImageClick(List<String> images, int position);
    }
    public class ImageAdapter extends BaseAdapter {
        private List<String> images;
        public ImageAdapter(List<String> images) {
            this.images = images;
        }
        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public Object getItem(int position) {
            return images.get(position);
        }

        @Override
        public long getItemId(int position) {
            return Integer.toUnsignedLong(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView iv;
            if(convertView == null) {
                iv = new ImageView(context);
                iv.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                iv = (ImageView) convertView;
            }
//            Glide.with(context)
//                    .load(images.get(position))
//                    .placeholder(R.mipmap.ic_default_image)  // 默认图
//                    .error(R.mipmap.ic_default_image)       // 错误时的默认图
//                    .into(iv);

            iv.setOnClickListener(v -> {
                onFeedActionListener.onImageClick(images, position);
            });
            return iv;
        }
    }

    @NonNull
    @Override
    public FeedItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
        return new FeedItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedItemViewHolder holder, int position) {
        FeedModel feedModel = feedModels.get(position);
        UserModel author = feedModel.getAuthor();
        holder.tvNickname.setText(author.getNickname());
        holder.tvTime.setText(feedModel.getCreateTime());
        holder.tvContent.setText(feedModel.getContent());
        // 设置头像

//        Glide.with(context)
//                .load(author.getAvatarUrl())
//                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
//                .placeholder(R.mipmap.ic_default_avatar)
//                .into(holder.ivAvatar);
        holder.tvNickname.setText(author.getNickname());
//        if (author.isVip()) {
//            holder.tvNickname.setTextColor(context.getResources().getColor(R.color.colorVipRed));
//        } else {
//            holder.tvNickname.setTextColor(context.getResources().getColor(R.color.colorTextPrimary));
//        }

        // 设置时间
        holder.tvTime.setText(feedModel.getCreateTime());

        // 设置内容
        holder.tvContent.setText(feedModel.getContent());

        // 设置图片
        List<String> imageUrls = feedModel.getImageUrls();
        if (imageUrls == null || imageUrls.isEmpty()) {
            holder.gvImages.setVisibility(View.GONE);
        } else {
            holder.gvImages.setVisibility(View.VISIBLE);
            if (imageUrls.size() == 1) {
                holder.gvImages.setNumColumns(1);
                ViewGroup.LayoutParams params = holder.gvImages.getLayoutParams();
                params.height = context.getResources().getDimensionPixelSize(R.dimen.feed_single_image_height);
                holder.gvImages.setLayoutParams(params);
            } else {
                holder.gvImages.setNumColumns(3);
                ViewGroup.LayoutParams params = holder.gvImages.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.gvImages.setLayoutParams(params);
            }
            holder.gvImages.setAdapter(new ImageAdapter(imageUrls));
        }

        // 设置互动数据
//        holder.tvLikeCount.setText(feedModel.getLikeCount() + " 人点赞");
//        holder.tvCommentCount.setText(feedModel.getCommentCount() + " 条评论");
//        holder.tvShareCount.setText(feedModel.getShareCount() + " 次转发");

        // 设置点赞状态
        if (feedModel.isLiked()) {
//            holder.btnLike.setCompoundDrawablesWithIntrinsicBounds(
//                    R.drawable.ic_like_red, 0, 0, 0);
            holder.btnLike.setTextColor(context.getResources().getColor(R.color.color_like_red));
        } else {
            holder.btnLike.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_like_gray, 0, 0, 0);
            holder.btnLike.setTextColor(context.getResources().getColor(R.color.color_text_secondary));
        }

        // 设置按钮点击事件
        holder.btnLike.setOnClickListener(v -> onFeedActionListener.onLikeClick(feedModel));
        holder.btnComment.setOnClickListener(v -> onFeedActionListener.onCommentClick(feedModel));
        holder.btnShare.setOnClickListener(v ->
                Toast.makeText(context, "转发动态", Toast.LENGTH_SHORT).show());

        // 设置评论区
        List<CommentModel> comments = feedModel.getComments();
        if (comments == null || comments.isEmpty()) {
            holder.llComments.setVisibility(View.GONE);
        } else {
            holder.llComments.setVisibility(View.VISIBLE);
            holder.llCommentList.removeAllViews();

            // 只显示前两条评论
            int showCount = Math.min(comments.size(), 2);
            for (int i = 0; i < showCount; i++) {
                CommentModel comment = comments.get(i);
                View commentView = LayoutInflater.from(context)
                        .inflate(R.layout.item_comment, holder.llCommentList, false);

                TextView tvNickname = commentView.findViewById(R.id.tv_comment_nickname);
                TextView tvReplyTo = commentView.findViewById(R.id.tv_reply_to);
                TextView tvContent = commentView.findViewById(R.id.tv_comment_content);
                TextView tvTime = commentView.findViewById(R.id.tv_comment_time);

                tvNickname.setText(comment.getUser().getNickname());
                tvContent.setText(comment.getContent());
                tvTime.setText(comment.getCreateTime());

                if (comment.getReplyTo() != null) {
                    tvReplyTo.setVisibility(View.VISIBLE);
                    tvReplyTo.setText("回复@" + comment.getReplyTo().getUser().getNickname() + "：");
                } else {
                    tvReplyTo.setVisibility(View.GONE);
                }

                holder.llCommentList.addView(commentView);
            }

            // 显示"查看更多"按钮
            if (comments.size() > 2) {
                holder.btnShowMoreComments.setVisibility(View.VISIBLE);
                holder.btnShowMoreComments.setOnClickListener(v ->
                        Toast.makeText(context, "查看全部" + feedModel.getCommentCount() + "条评论",
                                Toast.LENGTH_SHORT).show());
            } else {
                holder.btnShowMoreComments.setVisibility(View.GONE);
            }
        }

    }

    @Override
    public int getItemCount() {
        return  feedModels.size();
    }

    public static class FeedItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvNickname, tvTime, tvContent, tvLikeCount, tvCommentCount, tvShareCount;
        ImageView ivAvatar;
        GridView gvImages;
        Button btnLike, btnComment, btnShare, btnShowMoreComments;
        LinearLayout llComments, llCommentList;
        public FeedItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNickname = itemView.findViewById(R.id.tv_nickname);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            gvImages = itemView.findViewById(R.id.gv_images);

            btnLike = itemView.findViewById(R.id.btn_like);
            btnComment = itemView.findViewById(R.id.btn_comment);
            btnShare = itemView.findViewById(R.id.btn_share);
            btnShowMoreComments = itemView.findViewById(R.id.btn_show_more_comments);
            llComments = itemView.findViewById(R.id.ll_comments);
            llCommentList = itemView.findViewById(R.id.ll_comment_list);
        }
    }

    public void setOnFeedActionListener(OnFeedActionListener onFeedActionListener) {
        this.onFeedActionListener = onFeedActionListener;
    }
}
