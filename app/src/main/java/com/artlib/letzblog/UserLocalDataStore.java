package com.artlib.letzblog;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ARTLIB on 06/07/2017.
 */

public class UserLocalDataStore {

    public static final String SP_NAME = "blogDetails";
    SharedPreferences userLocalDataStore;

    public UserLocalDataStore(Context context) {
        userLocalDataStore = context.getSharedPreferences(SP_NAME, 0);
    }

    // Method to clear user data from the system when logged out
    public void clearUserData() {
        SharedPreferences.Editor editor = userLocalDataStore.edit();
        editor.clear();
        editor.commit();
    }

    public void storeProfileUsername(String username) {
        SharedPreferences.Editor editor = userLocalDataStore.edit();
        editor.putString("profileUsername", username);
        editor.commit();
    }

    public void storeProfilePicture(String imagePath) {
        SharedPreferences.Editor editor = userLocalDataStore.edit();
        editor.putString("profileImage",imagePath);
        editor.commit();
    }

    public String getProfileUsername() {
        String username = userLocalDataStore.getString("profileUsername", "");
        return username;
    }

    public String getProfilePicture() {
        String imagePath = userLocalDataStore.getString("profileImage", "");
        return imagePath;
    }

    public void storePostKey(String postKey) {
        SharedPreferences.Editor editor = userLocalDataStore.edit();
        editor.putString("postKey", postKey);
        editor.commit();
    }

    public String getPostKey() {
        String postKey = userLocalDataStore.getString("postKey", "");
        return postKey;
    }

    public void storeOpenPostUpdateFlag(boolean flag) {
        SharedPreferences.Editor editor = userLocalDataStore.edit();
        editor.putBoolean("updateFlag", flag);
        editor.commit();
    }

    public boolean getOpenPostUpdateFlag() {
        boolean flag = userLocalDataStore.getBoolean("updateFlag", false);
        return flag;
    }

    public void storeBlogImage(String image) {
        SharedPreferences.Editor editor = userLocalDataStore.edit();
        editor.putString("blogImage", image);
        editor.commit();
    }

    public String getBlogImage() {
        String image = userLocalDataStore.getString("blogImage", "");
        return image;
    }

}
