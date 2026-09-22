package com.choosethename.backend.functional;

import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.SharedNamePoolEntity;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Spec004FunctionalTest extends FunctionalTestBase {

    @Test
    void fullVotingFlowFromSelectionToResults() {
        String alice = registerAndLogin("alice_vote");
        String bob = registerAndLogin("bob_vote");
        String carol = registerAndLogin("carol_vote");
        int listId = createVotingList(alice, bob);

        adopt(alice, listId, "lucia");
        adopt(alice, listId, "sofia");
        adopt(bob, listId, "maria");
        adopt(bob, listId, "juan");

        completeSelection(alice, listId);
        completeSelection(bob, listId);

        Response active = client.getListById(alice, listId);
        active.then().statusCode(200)
                .body("phase", org.hamcrest.Matchers.equalTo("VOTING"))
                .body("currentRound", org.hamcrest.Matchers.is(1))
                .body("totalRounds", org.hamcrest.Matchers.is(2));
        assertThat(active.jsonPath().getList("currentPool")).containsExactly("lucia", "sofia", "maria", "juan");

        // Results not ready while VOTING => 409
        client.getResults(alice, listId).then().statusCode(409);

        // Non-members are forbidden => 403
        client.submitVote(carol, listId, 1, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(403);
        client.getResults(carol, listId).then().statusCode(403);

        // Partial ranking => 400, duplicate => 422, stale round => 409
        client.submitVote(alice, listId, 1, List.of("lucia", "sofia", "maria")).then().statusCode(400);
        client.submitVote(alice, listId, 1, List.of("lucia", "lucia", "maria", "juan")).then().statusCode(422);
        client.submitVote(alice, listId, 0, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(409);

        // One vote does not advance the round
        client.submitVote(alice, listId, 1, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(200);
        assertThat(client.getListById(alice, listId).jsonPath().getInt("currentRound")).isEqualTo(1);

        // Re-vote overwrites without advancing
        client.submitVote(alice, listId, 1, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(200);

        // Second vote advances to round 2 (cap 5 keeps all four names)
        client.submitVote(bob, listId, 1, List.of("maria", "juan", "lucia", "sofia")).then().statusCode(200);
        active = client.getListById(alice, listId);
        active.then().statusCode(200)
                .body("currentRound", org.hamcrest.Matchers.is(2));
        assertThat(active.jsonPath().getList("currentPool")).containsExactly("lucia", "maria", "juan", "sofia");

        // Voting for a finalized round => 409
        client.submitVote(alice, listId, 1, List.of("lucia", "sofia", "maria", "juan")).then().statusCode(409);

        // Final round: second vote completes the list
        client.submitVote(alice, listId, 2, List.of("lucia", "maria", "juan", "sofia")).then().statusCode(200);
        client.submitVote(bob, listId, 2, List.of("lucia", "maria", "sofia", "juan")).then().statusCode(200);
        assertThat(listRepository.findById((long) listId).orElseThrow().getPhase()).isEqualTo(ListPhase.COMPLETED);

        // Top-3 with consolidated scores
        Response results = client.getResults(alice, listId);
        results.then().statusCode(200);
        assertThat(results.jsonPath().getInt("results[0].rank")).isEqualTo(1);
        assertThat(results.jsonPath().getString("results[0].name")).isEqualTo("lucia");
        assertThat(results.jsonPath().getInt("results[0].score")).isEqualTo(6);
        assertThat(results.jsonPath().getString("results[1].name")).isEqualTo("maria");
        assertThat(results.jsonPath().getInt("results[1].score")).isEqualTo(4);
        assertThat(results.jsonPath().getString("results[2].name")).isEqualTo("juan");
        assertThat(results.jsonPath().getInt("results[2].score")).isEqualTo(1);
        assertThat(results.jsonPath().getList("results")).hasSize(3);
    }

    @Test
    void largePoolUsesThreeRoundsAndEliminates() {
        String alice = registerAndLogin("alice_large");
        String bob = registerAndLogin("bob_large");
        int listId = createVotingList(alice, bob);

        for (int i = 0; i < 16; i++) {
            addPoolEntry((long) listId, "alice_large", "name" + i);
        }
        completeSelection(alice, listId);
        completeSelection(bob, listId);

        Response active = client.getListById(alice, listId);
        active.then().statusCode(200)
                .body("phase", org.hamcrest.Matchers.equalTo("VOTING"))
                .body("totalRounds", org.hamcrest.Matchers.is(3));
        assertThat(active.jsonPath().getList("currentPool")).hasSize(16);

        voteAndAdvance(alice, bob, listId, 1);
        assertThat(client.getListById(alice, listId).jsonPath().getList("currentPool")).hasSize(10);

        voteAndAdvance(alice, bob, listId, 2);
        assertThat(client.getListById(alice, listId).jsonPath().getList("currentPool")).hasSize(5);

        List<String> finalPool = client.getListById(alice, listId).jsonPath().getList("currentPool");
        client.submitVote(alice, listId, 3, finalPool).then().statusCode(200);
        client.submitVote(bob, listId, 3, finalPool).then().statusCode(200);

        assertThat(listRepository.findById((long) listId).orElseThrow().getPhase()).isEqualTo(ListPhase.COMPLETED);
        Response results = client.getResults(alice, listId);
        results.then().statusCode(200);
        assertThat(results.jsonPath().getList("results")).hasSize(3);
        assertThat(results.jsonPath().getInt("results[0].rank")).isEqualTo(1);
        assertThat(results.jsonPath().getInt("results[1].rank")).isEqualTo(2);
        assertThat(results.jsonPath().getInt("results[2].rank")).isEqualTo(3);
    }

    private int createVotingList(String alice, String bob) {
        Response created = client.createList(alice, "Voting List");
        created.then().statusCode(201);
        int listId = created.jsonPath().getInt("id");
        String code = created.jsonPath().getString("invitationCode");
        client.joinList(bob, code).then().statusCode(200);

        client.addNames(alice, listId, List.of("pablo", "maria", "juan")).then().statusCode(200);
        client.addNames(bob, listId, List.of("pablo", "lucia", "sofia")).then().statusCode(200);
        client.finishAddition(alice, listId).then().statusCode(200);
        client.finishAddition(bob, listId).then().statusCode(200);
        return listId;
    }

    private void voteAndAdvance(String alice, String bob, int listId, int round) {
        List<String> pool = client.getListById(alice, listId).jsonPath().getList("currentPool");
        List<String> reversed = new ArrayList<>(pool);
        Collections.reverse(reversed);

        client.submitVote(alice, listId, round, pool).then().statusCode(200);
        client.submitVote(bob, listId, round, reversed).then().statusCode(200);
    }

    private void adopt(String token, int listId, String name) {
        client.adoptName(token, listId, name).then().statusCode(200);
    }

    private void completeSelection(String token, int listId) {
        client.completeSelection(token, listId).then().statusCode(200);
    }

    private void addPoolEntry(Long listId, String adopterUsername, String normalizedName) {
        Long adopterId = userRepository.findByUsername(adopterUsername).orElseThrow().getId();
        SharedNamePoolEntity entry = new SharedNamePoolEntity();
        entry.setListId(listId);
        entry.setNormalizedName(normalizedName);
        entry.setAdoptedBy(adopterId);
        entry.setAdoptedAt(Instant.now());
        sharedNamePoolRepository.save(entry);
    }
}