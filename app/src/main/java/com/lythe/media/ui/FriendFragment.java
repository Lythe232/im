package com.lythe.media.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lythe.media.R;
import com.lythe.media.ui.adapter.FriendListAdapter;
import com.lythe.media.ui.adapter.LetterIndexAdapter;
import com.lythe.media.chats.data.model.FriendListItem;
import com.lythe.media.chats.utils.FriendListHelper;
import com.lythe.media.chats.viewmodel.FriendListViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FriendFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendFragment extends Fragment {
    private final static String TAG = "FriendFragment";
    private RecyclerView friendRecyclerView;
    private RecyclerView letterIndexRecyclerView;

    private FriendListAdapter friendListAdapter;
    private LetterIndexAdapter letterIndexAdapter;

    private FriendListViewModel viewModel;
    private List<String> letterList;

    public FriendFragment() {
        // Required empty public constructor
    }
    public static FriendFragment newInstance(String param1, String param2) {
        FriendFragment fragment = new FriendFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FriendListViewModel.class);
        viewModel.initLoad();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend, container, false);
        initView(view);
        FriendFragment that = this;
        friendListAdapter = new FriendListAdapter(getContext(), new ArrayList<>());
        friendListAdapter.setOnItemClickListener(new FriendListAdapter.OnItemClickListener() {
            @Override
            public void onclick(FriendListItem item, View v) {
                if(item == null || TextUtils.isEmpty(item.getFriend().getId())) {
                    Toast.makeText(that.getContext(), "数据异常，无法查看详情", Toast.LENGTH_SHORT).show();
                    return ;
                }
                Intent intent = new Intent(FriendFragment.this.getContext(), FriendProfileCardActivity.class);
                intent.putExtra("id", item.getFriend().getId());
                intent.putExtra("username", item.getFriend().getUsername());
                intent.putExtra("avatar", item.getFriend().getAvatar());
                intent.putExtra("sign", item.getFriend().getSignature());
                startActivity(intent);
            }
        });
        letterIndexAdapter = new LetterIndexAdapter(new ArrayList<>(), letter -> {
            // 根据字母快速跳转到好友列表
            List<FriendListItem> items = friendListAdapter.getItems();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getFriend().getLetter().equals(letter)) {
                    friendRecyclerView.scrollToPosition(i);
                    break;
                }
            }
        });
        friendRecyclerView.setAdapter(friendListAdapter);
        letterIndexRecyclerView.setAdapter(letterIndexAdapter);
        viewModel.getFriendListItems().observe(getViewLifecycleOwner(), friendModels -> {
            List<FriendListItem> friendListItems = FriendListHelper.groupFriendsByLetter(friendModels);
            List<String> letters = FriendListHelper.getLetters(friendModels);

            friendListAdapter.updateList(friendListItems);
            letterIndexAdapter.updateList(letters);
        });
        viewModel.refreshFriends();

        return view;
    }

    public void initView(View view) {
        friendRecyclerView = view.findViewById(R.id.friend_recycler_view);
        letterIndexRecyclerView = view.findViewById(R.id.letter_index_recycler_view);
        friendRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        letterIndexRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}