package com.lythe.media.chats.utils;

import com.lythe.media.chats.data.model.FriendListItem;
import com.lythe.media.chats.data.model.FriendModel;
import com.lythe.media.chats.data.model.ItemType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FriendListHelper {
    public static List<FriendListItem> groupFriendsByLetter(List<FriendModel> friends) {

        List<FriendListItem> result = new ArrayList<>();
        List<FriendModel> sortedFriends = friends.stream()
                .sorted(
                        (f1, f2)
                                -> CharSequence.compare(f1.getLetter(), f2.getLetter()))
                .collect(Collectors.toList());

        String currentLetter = null;
        for(FriendModel friendModel : sortedFriends) {
            if(!Objects.equals(friendModel.getLetter(), currentLetter)) {
                currentLetter = friendModel.getLetter();
                result.add(new FriendListItem(
                        ItemType.LETTER_TITLE,
                        friendModel
                ));
            }
            result.add(new FriendListItem(
                    ItemType.FRIEND_ITEM,
                    friendModel
            ));
        }
        return result;
    }

    public static List<String> getLetters(List<FriendModel> friendModels) {
        Set<String> lettersSet = friendModels.stream()
                .map(FriendModel::getLetter)
                .collect(Collectors.toSet());
        ArrayList<String> strings = new ArrayList<>(lettersSet);
        strings.sort(String::compareTo);

        return strings;
    }
}
