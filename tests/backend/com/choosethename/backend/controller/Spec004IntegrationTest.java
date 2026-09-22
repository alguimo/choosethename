package com.choosethename.backend.controller;

import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.SharedNamePoolEntity;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.NameRepository;
import com.choosethename.backend.repository.SharedNamePoolRepository;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.repository.VoteRepository;
import com.choosethename.backend.repository.VotingRoundRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Spec004IntegrationTest {

    private static final String PASSWORD = "Password123";

    @LocalServerPort
    private int port;
    @Autowired private UserRepository userRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private ListMembershipRepository membershipRepository;
    @Autowired private NameRepository nameRepository;
    @Autowired private SharedNamePoolRepository sharedNamePoolRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VotingRoundRepository votingRoundRepository;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        voteRepository.deleteAll();
        votingRoundRepository.deleteAll();
        sharedNamePoolRepository.deleteAll();
        nameRepository.deleteAll();
        membershipRepository.deleteAll();
        listRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullVotingFlowFromSelectionToResults() {
        String alice = registerAndLogin("alice_004");
        String bob = registerAndLogin("bob_004");
        String carol = registerAndLogin("carol_004");
        int listId = createVotingList(alice, bob);

        adopt(alice, listId, "lucia");
        adopt(alice, listId, "sofia");
        adopt(bob, listId, "maria");
        adopt(bob, listId, "juan");

        completeSelection(alice, listId);
        completeSelection(bob, listId);

        Response active = getList(alice, listId);
        assertThat(active.jsonPath().getString("phase")).isEqualTo("VOTING");
        assertThat(active.jsonPath().getInt("currentRound")).isEqualTo(1);
        assertThat(active.jsonPath().getInt("totalRounds")).isEqualTo(2);
        assertThat(active.jsonPath().getList("currentPool")).containsExactly("lucia", "sofia", "maria", "juan");

        // Results are not ready while VOTING
        getResults(alice, listId).then().statusCode(409);

        // Non-members are forbidden from voting while VOTING
        vote(carol, listId, 1, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(403);

        // Partial ranking => 400
        vote(alice, listId, 1, List.of("lucia", "sofia", "maria")).then().statusCode(400);

        // Duplicate ranking => 422
        vote(alice, listId, 1, List.of("lucia", "lucia", "maria", "juan")).then().statusCode(422);

        // Stale round => 409
        vote(alice, listId, 0, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(409);

        // One of two votes does not advance the round
        vote(alice, listId, 1, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(200);
        assertThat(getList(alice, listId).jsonPath().getInt("currentRound")).isEqualTo(1);

        // Re-vote overwrites without advancing
        vote(alice, listId, 1, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(200);
        assertThat(voteRepository.countByListIdAndRoundId((long) listId,
                votingRoundRepository.findByListIdAndRoundNumber((long) listId, 1).orElseThrow().getId())).isEqualTo(1);

        // Second vote advances to round 2 (caps keep all four names)
        vote(bob, listId, 1, List.of("maria", "juan", "lucia", "sofia")).then().statusCode(200);
        active = getList(alice, listId);
        assertThat(active.jsonPath().getString("phase")).isEqualTo("VOTING");
        assertThat(active.jsonPath().getInt("currentRound")).isEqualTo(2);
        assertThat(active.jsonPath().getList("currentPool")).containsExactly("lucia", "maria", "juan", "sofia");

        // Voting for the previous round after advancing => 409
        vote(alice, listId, 1, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(409);

        // Final round: second vote completes the list
        vote(alice, listId, 2, List.of("lucia", "maria", "juan", "sofia")).then().statusCode(200);
        vote(bob, listId, 2, List.of("lucia", "maria", "sofia", "juan")).then().statusCode(200);

        assertThat(listRepository.findById((long) listId).orElseThrow().getPhase()).isEqualTo(ListPhase.COMPLETED);

        Response results = getResults(alice, listId);
        results.then().statusCode(200);
        assertThat(results.jsonPath().getInt("results[0].rank")).isEqualTo(1);
        assertThat(results.jsonPath().getString("results[0].name")).isEqualTo("lucia");
        assertThat(results.jsonPath().getInt("results[0].score")).isEqualTo(6);
        assertThat(results.jsonPath().getString("results[1].name")).isEqualTo("maria");
        assertThat(results.jsonPath().getInt("results[1].score")).isEqualTo(4);
        assertThat(results.jsonPath().getString("results[2].name")).isEqualTo("juan");
        assertThat(results.jsonPath().getInt("results[2].score")).isEqualTo(1);
        assertThat(results.jsonPath().getList("results")).hasSize(3);

        // Non-members are forbidden from reading results
        getResults(carol, listId).then().statusCode(403);
    }

    @Test
    void largePoolUsesThreeRoundsAndEliminates() {
        String alice = registerAndLogin("alice_large");
        String bob = registerAndLogin("bob_large");
        int listId = createVotingList(alice, bob);

        for (int i = 0; i < 16; i++) {
            addPoolEntry((long) listId, "name" + i);
        }

        completeSelection(alice, listId);
        completeSelection(bob, listId);

        Response active = getList(alice, listId);
        assertThat(active.jsonPath().getString("phase")).isEqualTo("VOTING");
        assertThat(active.jsonPath().getInt("totalRounds")).isEqualTo(3);
        assertThat(active.jsonPath().getList("currentPool")).hasSize(16);

        voteAndAdvance(alice, bob, listId, 1, 16);
        assertThat(getList(alice, listId).jsonPath().getList("currentPool")).hasSize(10);

        voteAndAdvance(alice, bob, listId, 2, 10);
        assertThat(getList(alice, listId).jsonPath().getList("currentPool")).hasSize(5);

        List<String> finalPool = getList(alice, listId).jsonPath().getList("currentPool");
        vote(alice, listId, 3, finalPool).then().statusCode(200);
        vote(bob, listId, 3, finalPool).then().statusCode(200);

        assertThat(listRepository.findById((long) listId).orElseThrow().getPhase()).isEqualTo(ListPhase.COMPLETED);
        assertThat(getResults(alice, listId).jsonPath().getList("results")).hasSize(3);
    }

    private int createVotingList(String alice, String bob) {
        Response created = createList(alice, "Voting List");
        created.then().statusCode(201);
        int listId = created.jsonPath().getInt("id");
        String code = created.jsonPath().getString("invitationCode");
        joinList(bob, code).then().statusCode(200);

        addNames(alice, listId, List.of("pablo", "maria", "juan")).then().statusCode(200);
        addNames(bob, listId, List.of("pablo", "lucia", "sofia")).then().statusCode(200);
        finishAddition(alice, listId);
        finishAddition(bob, listId);
        return listId;
    }

    private void voteAndAdvance(String alice, String bob, int listId, int round, int expectedPoolSize) {
        List<String> pool = getList(alice, listId).jsonPath().getList("currentPool");
        assertThat(pool).hasSize(expectedPoolSize);
        List<String> reversed = new ArrayList<>(pool);
        Collections.reverse(reversed);

        vote(alice, listId, round, pool).then().statusCode(200);
        vote(bob, listId, round, reversed).then().statusCode(200);
    }

    private void addPoolEntry(Long listId, String normalizedName) {
        SharedNamePoolEntity entry = new SharedNamePoolEntity();
        entry.setListId(listId);
        entry.setNormalizedName(normalizedName);
        entry.setAdoptedBy(userRepository.findAll().get(0).getId());
        entry.setAdoptedAt(Instant.now());
        sharedNamePoolRepository.save(entry);
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

    private void finishAddition(String token, int listId) {
        given()
            .header("Authorization", "Bearer " + token)
            .post("/api/v1/lists/" + listId + "/finish-addition")
            .then().statusCode(200);
    }

    private void completeSelection(String token, int listId) {
        given()
            .header("Authorization", "Bearer " + token)
            .post("/api/v1/lists/" + listId + "/complete-selection")
            .then().statusCode(200);
    }

    private void adopt(String token, int listId, String name) {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("name", name))
            .post("/api/v1/lists/" + listId + "/selection/adopt")
            .then().statusCode(200);
    }

    private Response getList(String token, int listId) {
        return given()
            .header("Authorization", "Bearer " + token)
            .get("/api/v1/lists/" + listId);
    }

    private Response vote(String token, int listId, int round, List<String> rankings) {
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("roundNumber", round, "rankings", rankings))
            .post("/api/v1/lists/" + listId + "/vote");
    }

    private Response getResults(String token, int listId) {
        return given()
            .header("Authorization", "Bearer " + token)
            .get("/api/v1/lists/" + listId + "/results");
    }
}