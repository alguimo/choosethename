package com.choosethename.backend.functional;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class RestAssuredSystemClient implements SystemClient {

    public RestAssuredSystemClient(int port) {
        RestAssured.port = port;
    }

    @Override
    public int register(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/api/v1/auth/register")
                .getStatusCode();
    }

    @Override
    public String login(String username, String password) {
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/api/v1/auth/login");

        if (response.getStatusCode() == 200) {
            return response.jsonPath().getString("accessToken");
        }
        return null;
    }

    @Override
    public int getProtectedResource(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/test/protected")
                .getStatusCode();
    }
}