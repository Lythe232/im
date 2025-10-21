package com.lythe.media.chats.data.model;

public class FriendListItem {

    ItemType itemType;
    FriendModel friend;

    public FriendListItem(ItemType itemType,  FriendModel friend) {
        this.itemType = itemType;
        this.friend = friend;
    }

    public ItemType getItemType() {
        return itemType;
    }


    public FriendModel getFriend() {
        return friend;
    }

    public void setFriend(FriendModel friend) {
        this.friend = friend;
    }
}
