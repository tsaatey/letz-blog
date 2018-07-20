package com.artlib.letzblog;

/**
 * Created by ARTLIB on 31/07/2017.
 */

public class Blog {

    private String blogTitle;
    private String blogDescription;
    private String blogImage;
    private String profileUsername;
    private String profileImage;
    private String blogDate;

    public Blog() {
    }

    public Blog(String blogTitle, String blogDescription, String blogImage, String profileUsername, String profileImage, String blogDate) {
        this.blogTitle = blogTitle;
        this.blogDescription = blogDescription;
        this.blogImage = blogImage;
        this.profileUsername = profileUsername;
        this.profileImage = profileImage;
        this.blogDate = blogDate;
    }

    public String getBlogTitle() {
        return blogTitle;
    }

    public void setBlogTitle(String blogTitle) {
        this.blogTitle = blogTitle;
    }

    public String getBlogDescription() {
        return blogDescription;
    }

    public void setBlogDescription(String blogDescription) {
        this.blogDescription = blogDescription;
    }

    public String getBlogImage() {
        return blogImage;
    }

    public void setBlogImage(String blogImage) {
        this.blogImage = blogImage;
    }

    public String getProfileUsername() {
        return profileUsername;
    }

    public void setProfileUsername(String profileUsername) {
        this.profileUsername = profileUsername;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getBlogDate() {
        return blogDate;
    }

    public void setBlogDate(String blogDate) {
        this.blogDate = blogDate;
    }
}
