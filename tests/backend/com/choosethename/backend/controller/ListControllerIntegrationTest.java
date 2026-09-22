package com.choosethename.backend.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ListControllerIntegrationTest {

    private static final String PASSWORD = "Password123";

    @LocalServerPort
    private int port;
    @Autowired private UserRepository userRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private ListMembershipRepository membershipRepository;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        membershipRepository.deleteAll();
        listRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(String username) {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", username, "password", PASSWORD))
            .post("/api/v1/auth/register");
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", username, "password", PASSWORD))
            .post("/api/v1/auth/login")
            .jsonPath()
            .getString("accessToken");
    }

    private Response createList(String token, String name) {
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("name", name))
            .post("/api/v1/lists");
    }

    private Response joinList(String token, String code) {
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("code", code))
            .post("/api/v1/lists/join");
    }

    @Test
    void testSpec002Scenarios() {
        String alvaro = registerAndLogin("alvaro_ts");
        String maria = registerAndLogin("maria_ts");
        String luis = registerAndLogin("luis_ts");
        String pedro = registerAndLogin("pedro_ts");
        String juan = registerAndLogin("juan_ts");
        String carlos = registerAndLogin("carlos_ts");
        String david = registerAndLogin("david_ts");
        String elena = registerAndLogin("elena_ts");
        String francisco = registerAndLogin("francisco_ts");
        String gema = registerAndLogin("gema_ts");

        // --- TS-1: Create list successfully ---
        Response created = createList(alvaro, "Baby Names 2026");
        created.then().statusCode(201);
        String code = created.jsonPath().getString("invitationCode");
        int alvaroListId = created.jsonPath().getInt("id");
        assertThat(code).hasSize(6).matches("^[A-Z0-9]{6}$");
        assertThat(Instant.parse(created.jsonPath().getString("codeExpiresAt")))
                .isAfter(Instant.now().plusSeconds(47L * 3600));
        assertThat(created.jsonPath().getString("phase")).isEqualTo("ADDITION");
        assertThat(created.jsonPath().getBoolean("invitationsOpen")).isTrue();
        assertThat(created.jsonPath().getString("ownerUsername")).isEqualTo("alvaro_ts");
        assertThat(created.jsonPath().getList("members")).containsExactly("alvaro_ts");

        // --- TS-2: Create another list while already belonging to one (201) ---
        Response secondList = createList(alvaro, "Second List");
        secondList.then().statusCode(201);
        int secondListId = secondList.jsonPath().getInt("id");

        // --- TS-3: Create with blank name (400) ---
        createList(maria, "   ").then().statusCode(400);
        createList(maria, "").then().statusCode(400);

        // --- TS-5: Join with malformed code (400) ---
        joinList(maria, "AB12").then().statusCode(400);
        joinList(maria, "ABCDEFG").then().statusCode(400);

        // --- TS-6: Join with non-existent code (404) ---
        joinList(luis, "NOPE99").then().statusCode(404);

        // --- TS-4: Join with valid code (200, member added) ---
        Response joined = joinList(maria, code.toLowerCase());
        joined.then().statusCode(200);
        assertThat(joined.jsonPath().getBoolean("invitationsOpen")).isTrue();
        assertThat(joined.jsonPath().getList("members")).contains("alvaro_ts", "maria_ts");

        // --- TS-10: Join again when already a member (400) ---
        joinList(maria, code).then().statusCode(400);

        // --- TS-7: Join with expired code (400) ---
        Response pedroCreated = createList(pedro, "Pedro List");
        pedroCreated.then().statusCode(201);
        String pedroCode = pedroCreated.jsonPath().getString("invitationCode");
        int pedroListId = pedroCreated.jsonPath().getInt("id");
        ListEntity expiredList = listRepository.findById((long) pedroListId).orElseThrow();
        expiredList.setCodeExpiresAt(Instant.now().minusSeconds(60));
        listRepository.save(expiredList);
        joinList(juan, pedroCode).then().statusCode(400);

        // --- Join members 3 and 4 ---
        joinList(carlos, code).then().statusCode(200);
        joinList(david, code).then().statusCode(200);

        // --- TS-9: Joining the 5th member auto-closes invitations ---
        Response fifth = joinList(elena, code);
        fifth.then().statusCode(200);
        assertThat(fifth.jsonPath().getBoolean("invitationsOpen")).isFalse();

        // --- TS-8: Join when list already has 5 members (400) ---
        joinList(francisco, code).then().statusCode(400);

        // --- TS-7/Spec: Join after invitations closed (400) ---
        joinList(gema, code).then().statusCode(400);

        // --- TS-11: Owner closes invitations (200, invitationsOpen false) ---
        Response closed = given()
            .header("Authorization", "Bearer " + alvaro)
            .patch("/api/v1/lists/" + alvaroListId + "/close-invitations");
        closed.then().statusCode(200);
        assertThat(closed.jsonPath().getBoolean("invitationsOpen")).isFalse();

        // --- TS-12: Non-owner member cannot close invitations (403) ---
        given()
            .header("Authorization", "Bearer " + maria)
            .patch("/api/v1/lists/" + alvaroListId + "/close-invitations")
            .then().statusCode(403);

        // --- TS-15: Fetch a list by id as a member with complete details (200) ---
        Response detail = given()
            .header("Authorization", "Bearer " + alvaro)
            .get("/api/v1/lists/" + alvaroListId);
        detail.then().statusCode(200);
        assertThat(detail.jsonPath().getInt("id")).isEqualTo(alvaroListId);
        assertThat(detail.jsonPath().getString("name")).isEqualTo("Baby Names 2026");
        assertThat(detail.jsonPath().getString("ownerUsername")).isEqualTo("alvaro_ts");
        assertThat(detail.jsonPath().getString("phase")).isEqualTo("ADDITION");
        assertThat(detail.jsonPath().getString("invitationCode")).isEqualTo(code);
        assertThat(detail.jsonPath().getBoolean("invitationsOpen")).isFalse();
        assertThat(detail.jsonPath().getList("members"))
                .containsExactlyInAnyOrder("alvaro_ts", "maria_ts", "carlos_ts", "david_ts", "elena_ts");

        // --- TS-14: Fetch my lists returns every list the user belongs to (200) ---
        Response myLists = given()
            .header("Authorization", "Bearer " + alvaro)
            .get("/api/v1/lists");
        myLists.then().statusCode(200);
        assertThat(myLists.jsonPath().getList("id")).contains(alvaroListId, secondListId);

        // --- TS-13: A user who belongs to no list gets an empty array (200) ---
        given()
            .header("Authorization", "Bearer " + gema)
            .get("/api/v1/lists")
            .then().statusCode(200).body("$", org.hamcrest.Matchers.empty());

        // --- TS-16: Fetch a list as a non-member (404) ---
        given()
            .header("Authorization", "Bearer " + gema)
            .get("/api/v1/lists/" + alvaroListId)
            .then().statusCode(404);

        // --- TS-17: Fetch a non-existent list (404) ---
        given()
            .header("Authorization", "Bearer " + alvaro)
            .get("/api/v1/lists/999999")
            .then().statusCode(404);

        // --- NFR-1: Endpoints require valid JWT (401) ---
        given().contentType(ContentType.JSON)
            .body(Map.of("name", "No Auth"))
            .post("/api/v1/lists")
            .then().statusCode(401);
        given().get("/api/v1/lists").then().statusCode(401);
        given().get("/api/v1/lists/" + alvaroListId).then().statusCode(401);

        // --- NFR-1/Spec: users only fetch lists they belong to (404 when not a member) ---
        given()
            .header("Authorization", "Bearer " + juan)
            .get("/api/v1/lists/" + alvaroListId)
            .then().statusCode(404);
    }
}