package com.choosethename.backend.functional;

public interface SystemClient {
    int register(String username, String password);
    String login(String username, String password);
    int getProtectedResource(String token);
}
