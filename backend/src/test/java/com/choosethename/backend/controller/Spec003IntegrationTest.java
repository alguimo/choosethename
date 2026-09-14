package com.choosethename.backend.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.NameRepository;
import com.choosethename.backend.repository.SharedNamePoolRepository;
import com.choosethename.backend.repository.UserRepository;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Spec003IntegrationTest {

    private static final String PASSWORD = "Password123";

    @LocalServerPort
    private int port;
    @Autowired private UserRepository userRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private ListMembershipRepository membershipRepository;
    @Autowired private NameRepository nameRepository;
    @Autowired private SharedNamePoolRepository sharedNamePoolRepository;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        sharedNamePoolRepository.deleteAll();
        nameRepository.deleteAll();
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

    private Response addNames(String token, int listId, List<String> names) {
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("names", names))
            .post("/api/v1/lists/" + listId + "/names");
    }

    @Test
    void testSpec003AddNamesAndFinishFlow() {
        String alice = registerAndLogin("alice_003");
        String bob = registerAndLogin("bob_003");

        Response created = createList(alice, "Baby Names 003");
        created.then().statusCode(201);
        int listId = created.jsonPath().getInt("id");
        String code = created.jsonPath().getString("invitationCode");
        joinList(bob, code).then().statusCode(200);

        // FR-2 / NFR-1: add names returns normalized values
        Response added = addNames(alice, listId, List.of("  Pablo  ", "Lucia"));
        added.then().statusCode(200);
        assertThat(added.jsonPath().getString("names[0].normalizedName")).isEqualTo("pablo");
        assertThat(added.jsonPath().getString("names[1].normalizedName")).isEqualTo("lucia");

        // FR-3: duplicate normalized name => 422
        addNames(alice, listId, List.of("pablo")).then()
            .statusCode(422)
            .body("error", org.hamcrest.Matchers.equalTo("Name already exists for this user in this list"));

        // Edge case: blank after normalization => 422
        addNames(alice, listId, List.of("   ")).then()
            .statusCode(422)
            .body("error", org.hamcrest.Matchers.equalTo("Name cannot be blank"));

        // FR-4: one finish is not enough
        given().header("Authorization", "Bearer " + alice)
            .post("/api/v1/lists/" + listId + "/finish-addition")
            .then().statusCode(200);
        given().header("Authorization", "Bearer " + alice)
            .get("/api/v1/lists/active")
            .then().statusCode(200)
            .body("phase", org.hamcrest.Matchers.equalTo("ADDITION"));

        // bob adds names and finishes
        addNames(bob, listId, List.of("Pablo", "Maria")).then().statusCode(200);
        given().header("Authorization", "Bearer " + bob)
            .post("/api/v1/lists/" + listId + "/finish-addition")
            .then().statusCode(200);

        // FR-4: both finished => SELECTION and invitations auto-closed (spec 002 FR-14)
        given().header("Authorization", "Bearer " + alice)
            .get("/api/v1/lists/active")
            .then().statusCode(200)
            .body("phase", org.hamcrest.Matchers.equalTo("SELECTION"))
            .body("invitationsOpen", org.hamcrest.Matchers.is(false));

        // FR-5: common names + faded suggestions
        Response selection = given()
            .header("Authorization", "Bearer " + alice)
            .get("/api/v1/lists/" + listId + "/selection");
        selection.then().statusCode(200);
        assertThat(selection.jsonPath().getList("commonNames.normalizedName")).containsExactly("pablo");
        assertThat(selection.jsonPath().getList("fadedSuggestions.normalizedName")).containsExactly("maria");
        assertThat(selection.jsonPath().getList("myNames.normalizedName")).containsExactlyInAnyOrder("pablo", "lucia");

        // FR-6: adopt faded suggestion (normalized)
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + alice)
            .body(Map.of("name", "  Maria  "))
            .post("/api/v1/lists/" + listId + "/selection/adopt")
            .then().statusCode(200);
        assertThat(sharedNamePoolRepository.countByListId((long) listId)).isEqualTo(1);

        // Cannot adopt a common name
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + alice)
            .body(Map.of("name", "Pablo"))
            .post("/api/v1/lists/" + listId + "/selection/adopt")
            .then().statusCode(400)
            .body("error", org.hamcrest.Matchers.equalTo("Name is not a faded suggestion for this user"));

        // FR-7: both complete selection => VOTING
        given().header("Authorization", "Bearer " + alice)
            .post("/api/v1/lists/" + listId + "/complete-selection")
            .then().statusCode(200);
        given().header("Authorization", "Bearer " + alice)
            .get("/api/v1/lists/active")
            .then().statusCode(200)
            .body("phase", org.hamcrest.Matchers.equalTo("SELECTION"));
        given().header("Authorization", "Bearer " + bob)
            .post("/api/v1/lists/" + listId + "/complete-selection")
            .then().statusCode(200);
        given().header("Authorization", "Bearer " + alice)
            .get("/api/v1/lists/active")
            .then().statusCode(200)
            .body("phase", org.hamcrest.Matchers.equalTo("VOTING"));
    }

    @Test
    void testSpec003FinishAdditionRejectedWithZeroNames() {
        String carol = registerAndLogin("carol_003");
        String dave = registerAndLogin("dave_003");

        Response created = createList(carol, "Zero Names");
        created.then().statusCode(201);
        int listId = created.jsonPath().getInt("id");
        String code = created.jsonPath().getString("invitationCode");
        joinList(dave, code).then().statusCode(200);

        // FR-9: finishing with an empty private pool => 400 with exact message
        given().header("Authorization", "Bearer " + carol)
            .post("/api/v1/lists/" + listId + "/finish-addition")
            .then().statusCode(400)
            .body("error", org.hamcrest.Matchers.equalTo("At least one name must be provided to proceed to the selection phase."));

        given().header("Authorization", "Bearer " + dave)
            .post("/api/v1/lists/" + listId + "/finish-addition")
            .then().statusCode(400)
            .body("error", org.hamcrest.Matchers.equalTo("At least one name must be provided to proceed to the selection phase."));

        given().header("Authorization", "Bearer " + carol)
            .get("/api/v1/lists/active")
            .then().statusCode(200)
            .body("phase", org.hamcrest.Matchers.equalTo("ADDITION"));
    }

    @Test
    void testSpec003RequiresAuthentication() {
        String alice = registerAndLogin("alice_auth");
        Response created = createList(alice, "Auth List");
        created.then().statusCode(201);
        int listId = created.jsonPath().getInt("id");

        addNames("invalid.token", listId, List.of("Pablo")).then().statusCode(401);
        given().get("/api/v1/lists/" + listId + "/selection").then().statusCode(401);
        given().post("/api/v1/lists/" + listId + "/finish-addition").then().statusCode(401);
        given().contentType(ContentType.JSON)
            .body(Map.of("name", "Pablo"))
            .post("/api/v1/lists/" + listId + "/selection/adopt")
            .then().statusCode(401);
    }
}