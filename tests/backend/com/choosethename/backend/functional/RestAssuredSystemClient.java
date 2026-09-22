package com.choosethename.backend.functional;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class RestAssuredSystemClient implements SystemClient {

    public RestAssuredSystemClient(int port) {
        RestAssured.port = port;
    }

    @Override
    public Response register(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/api/v1/auth/register");
    }

    @Override
    public Response login(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/api/v1/auth/login");
    }

    @Override
    public Response getProtectedResource(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/test/protected");
    }

    @Override
    public Response createList(String token, String name) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", name))
                .post("/api/v1/lists");
    }

    @Override
    public Response joinList(String token, String code) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("code", code))
                .post("/api/v1/lists/join");
    }

    @Override
    public Response getMyLists(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/lists");
    }

    @Override
    public Response getListById(String token, int listId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/lists/{id}", listId);
    }

    @Override
    public Response closeInvitations(String token, int listId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .patch("/api/v1/lists/{id}/close-invitations", listId);
    }

    @Override
    public Response addNames(String token, int listId, List<String> names) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("names", names))
                .post("/api/v1/lists/{id}/names", listId);
    }

    @Override
    public Response finishAddition(String token, int listId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .post("/api/v1/lists/{id}/finish-addition", listId);
    }

    @Override
    public Response getSelection(String token, int listId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/lists/{id}/selection", listId);
    }

    @Override
    public Response adoptName(String token, int listId, String name) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("name", name))
                .post("/api/v1/lists/{id}/selection/adopt", listId);
    }

    @Override
    public Response completeSelection(String token, int listId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .post("/api/v1/lists/{id}/complete-selection", listId);
    }

    @Override
    public Response submitVote(String token, int listId, int roundNumber, List<String> rankings) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("roundNumber", roundNumber, "rankings", rankings))
                .post("/api/v1/lists/{id}/vote", listId);
    }

    @Override
    public Response getResults(String token, int listId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/lists/{id}/results", listId);
    }
}