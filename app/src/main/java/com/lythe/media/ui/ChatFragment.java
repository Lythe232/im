package com.lythe.media.ui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lythe.media.chats.data.entity.ConversationEntity;
import com.lythe.media.databinding.FragmentChatBinding;
import com.lythe.media.ui.adapter.ChatListAdapter;
import com.lythe.media.chats.viewmodel.ChatViewModel;

import java.util.ArrayList;


public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private FragmentChatBinding binding_;
    private ChatViewModel viewModel_;
    public ChatFragment() {
    }
    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding_ = FragmentChatBinding.inflate(inflater, container, false);
        recyclerView = binding_.chatRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel_ = new ViewModelProvider(this).get(ChatViewModel.class);
        
        adapter = new ChatListAdapter(new ArrayList<>());

        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(chatItem -> {
            navigateToChatInfo(chatItem);
        });
        TextView noChatsTv = binding_.noChatsTextView;
        noChatsTv.setVisibility(GONE);
        viewModel_.getChatList().observe(getViewLifecycleOwner(), chatItems -> {
            adapter.updateChatList(chatItems);

            if(chatItems == null || chatItems.isEmpty()) {
                recyclerView.setVisibility(GONE);
                noChatsTv.setVisibility(VISIBLE);
            } else {
                recyclerView.setVisibility(VISIBLE);
                noChatsTv.setVisibility(GONE);
            }
        });
        viewModel_.loadChats();
        return binding_.getRoot();
    }

    private void navigateToChatInfo(ConversationEntity chatItem) {
        Intent intent = new Intent(getActivity(), ChatInfoActivity.class);
//        intent.putExtra("CHATID", chatItem);
        intent.putExtra("conversationEntity", chatItem);
        startActivity(intent);
    }

}