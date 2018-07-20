package com.artlib.letzblog;

/**
 * Created by ARTLIB on 02/08/2017.
 */

public class User {

    private String username;
    private String image;

    public User() {
    }

    public User(String username, String image) {
        this.username = username;
        this.image = image;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
