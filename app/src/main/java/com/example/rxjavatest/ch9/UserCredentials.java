package com.example.rxjavatest.ch9;

public class UserCredentials {
    public final User user;
    public final String accessTocken;

    public UserCredentials(User user, String accessTocken) {
        this.user = user;
        this.accessTocken = accessTocken;
    }
}
