package com.lythe.media.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lythe.media.R;
import com.lythe.media.ui.adapter.FeedAdapter;
import com.lythe.media.ui.chats.data.model.CommentModel;
import com.lythe.media.ui.chats.data.model.FeedModel;
import com.lythe.media.ui.chats.data.model.UserModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FeedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FeedFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private List<FeedModel> feedModels;



    public FeedFragment() {
        // Required empty public constructor
        feedModels = new ArrayList<>();
        List<FeedModel> feedList = new ArrayList<>();

        // 用户
        UserModel alice = new UserModel("u1", "https://example.com/avatar1.png", "Alice", true);
        UserModel bob = new UserModel("u2", "https://example.com/avatar2.png", "Bob", false);
        UserModel charlie = new UserModel("u3", "https://example.com/avatar3.png", "Charlie", false);
        UserModel diana = new UserModel("u4", "https://example.com/avatar4.png", "Diana", true);

        // 评论
        CommentModel c1 = new CommentModel("c1", bob, "好漂亮啊！", "2025-09-22 10:00", null);
        CommentModel c2 = new CommentModel("c2", charlie, "同感！", "2025-09-22 11:00", c1);
        CommentModel c3 = new CommentModel("c3", diana, "下次一起？", "2025-09-21 15:00", null);

        // Feed 1
        feedList.add(new FeedModel(
                "f1",
                alice,
                "今天去海边玩，天气真好～",
                Arrays.asList("https://example.com/img1.jpg"),
                "2025-09-22 09:00",
                120, 2, 5,
                true,
                Arrays.asList(c1, c2)
        ));

        // Feed 2
        feedList.add(new FeedModel(
                "f2",
                bob,
                "咖啡厅学习中 ☕",
                Arrays.asList("https://example.com/img2.jpg", "https://example.com/img3.jpg"),
                "2025-09-21 15:30",
                80, 1, 2,
                false,
                Collections.singletonList(c3)
        ));

        // Feed 3
        feedList.add(new FeedModel(
                "f3",
                charlie,
                "刚跑完步，出了一身汗 🏃",
                null,
                "2025-09-20 18:20",
                45, 0, 0,
                true,
                null
        ));

        // Feed 4
        feedList.add(new FeedModel(
                "f4",
                diana,
                "分享一本最近喜欢的小说 📖",
                Arrays.asList("https://example.com/book_cover.jpg"),
                "2025-09-19 21:10",
                62, 3, 1,
                false,
                Arrays.asList(c1)
        ));

        // Feed 5
        feedList.add(new FeedModel(
                "f5",
                alice,
                "旅游打卡 📸",
                Arrays.asList("https://example.com/travel1.jpg", "https://example.com/travel2.jpg"),
                "2025-09-18 14:00",
                200, 6, 10,
                true,
                Arrays.asList(c2, c3)
        ));
        feedModels = feedList;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FeedFragment newInstance(String param1, String param2) {
        FeedFragment fragment = new FeedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FeedAdapter(getContext(), feedModels, new FeedAdapter.OnFeedActionListener() {
            @Override
            public void onLikeClick(FeedModel feed) {

            }

            @Override
            public void onCommentClick(FeedModel feed) {

            }

            @Override
            public void onImageClick(List<String> images, int position) {

            }
        });
        recyclerView.setAdapter(adapter);
        return view;
    }
}