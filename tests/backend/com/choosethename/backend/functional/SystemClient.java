package com.choosethename.backend.functional;

import io.restassured.response.Response;

import java.util.List;

public interface SystemClient {
    Response register(String username, String password);
    Response login(String username, String password);
    Response getProtectedResource(String token);

    Response createList(String token, String name);
    Response joinList(String token, String code);
    Response getActiveList(String token);
    Response closeInvitations(String token, int listId);

    Response addNames(String token, int listId, List<String> names);
    Response finishAddition(String token, int listId);
    Response getSelection(String token, int listId);
    Response adoptName(String token, int listId, String name);
    Response completeSelection(String token, int listId);

    Response submitVote(String token, int listId, int roundNumber, List<String> rankings);
    Response getResults(String token, int listId);
}