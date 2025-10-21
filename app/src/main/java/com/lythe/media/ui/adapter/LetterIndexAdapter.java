package com.lythe.media.ui.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lythe.media.R;

import java.util.List;

public class LetterIndexAdapter extends RecyclerView.Adapter<LetterIndexAdapter.LetterViewHolder> {
    private List<String> letterList;
    private OnLetterClickListener letterClickListener;

    public LetterIndexAdapter(List<String> letterList, OnLetterClickListener onLetterClickListener) {
        this.letterList = letterList;
        this.letterClickListener = onLetterClickListener;
    }

    @NonNull
    @Override
    public LetterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_letter_index, parent, false);
        return new LetterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LetterViewHolder holder, int position) {
        holder.letterTextView.setText(letterList.get(position));
        holder.itemView.setOnClickListener(v -> {
            letterClickListener.onLetterClick(letterList.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return letterList.size();
    }

    public static class LetterViewHolder extends RecyclerView.ViewHolder {
        TextView letterTextView;
        public LetterViewHolder(@NonNull View itemView) {
            super(itemView);
            letterTextView = itemView.findViewById(R.id.letter);
            itemView.setBackgroundResource(R.drawable.selector_click_feedback);
            itemView.setFocusable(true);
            itemView.setClickable(true);
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<String> list) {
        this.letterList = list;
        notifyDataSetChanged();
    }
    public interface OnLetterClickListener {
        void onLetterClick(String letter);
    }
}
