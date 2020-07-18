package com.farizma.magnumfarizma;

public class Item {

    private String username;
    private String avatarUrl;

    public Item(String username, String avatarUrl) {
        this.username = username;
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() {
        return username;
    }

    public  String getAvatarUrl() {
        return avatarUrl;
    }
}
