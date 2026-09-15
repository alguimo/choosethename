package com.choosethename.backend.functional;

import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.service.ListPhaseTransitionService;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Spec003FunctionalTest extends FunctionalTestBase {

    @Autowired
    private ListPhaseTransitionService phaseTransitionService;

    @Test
    void nameAdditionAndSelectionJourney() {
        String alice = registerAndLogin("alice_fun");
        String bob = registerAndLogin("bob_fun");

        Response created = client.createList(alice, "Baby Names");
        created.then().statusCode(201);
        int listId = created.jsonPath().getInt("id");
        String code = created.jsonPath().getString("invitationCode");
        client.joinList(bob, code).then().statusCode(200);

        // FR-2/NFR-1: names are normalized server-side
        Response added = client.addNames(alice, listId, List.of("  Pablo  ", "Lucia"));
        added.then().statusCode(200);
        assertThat(added.jsonPath().getString("names[0].normalizedName")).isEqualTo("pablo");
        assertThat(added.jsonPath().getString("names[1].normalizedName")).isEqualTo("lucia");

        // FR-3: duplicate normalized name => 422 with English message
        client.addNames(alice, listId, List.of("pablo")).then()
                .statusCode(422)
                .body("error", org.hamcrest.Matchers.equalTo("Name already exists for this user in this list"));

        // Edge case: blank after normalization => 422
        client.addNames(alice, listId, List.of("   ")).then()
                .statusCode(422)
                .body("error", org.hamcrest.Matchers.equalTo("Name cannot be blank"));

        // FR-4: one finish does not transition yet
        client.finishAddition(alice, listId).then().statusCode(200);
        client.getActiveList(alice).then()
                .statusCode(200)
                .body("phase", org.hamcrest.Matchers.equalTo("ADDITION"));

        client.addNames(bob, listId, List.of("Pablo", "Maria")).then().statusCode(200);
        client.finishAddition(bob, listId).then().statusCode(200);

        // FR-4 + Spec 002 FR-14: both finished => SELECTION and invitations auto-closed
        client.getActiveList(alice).then()
                .statusCode(200)
                .body("phase", org.hamcrest.Matchers.equalTo("SELECTION"))
                .body("invitationsOpen", org.hamcrest.Matchers.is(false));

        // FR-5: common names vs faded suggestions vs my names
        Response selection = client.getSelection(alice, listId);
        selection.then().statusCode(200);
        assertThat(selection.jsonPath().getList("commonNames.normalizedName")).containsExactly("pablo");
        assertThat(selection.jsonPath().getList("fadedSuggestions.normalizedName")).containsExactly("maria");
        assertThat(selection.jsonPath().getList("myNames.normalizedName")).containsExactlyInAnyOrder("pablo", "lucia");

        // FR-6: adopt a faded suggestion (normalized)
        client.adoptName(alice, listId, "  Maria  ").then().statusCode(200);
        assertThat(sharedNamePoolRepository.countByListId((long) listId)).isEqualTo(1);

        // Common names cannot be adopted
        client.adoptName(alice, listId, "Pablo").then().statusCode(400)
                .body("error", org.hamcrest.Matchers.equalTo("Name is not a faded suggestion for this user"));

        // FR-7: both complete selection => VOTING
        client.completeSelection(alice, listId).then().statusCode(200);
        client.getActiveList(alice).then()
                .statusCode(200)
                .body("phase", org.hamcrest.Matchers.equalTo("SELECTION"));
        client.completeSelection(bob, listId).then().statusCode(200);
        client.getActiveList(alice).then()
                .statusCode(200)
                .body("phase", org.hamcrest.Matchers.equalTo("VOTING"));
    }

    @Test
    void finishAdditionWithZeroNamesIsRejected() {
        String carol = registerAndLogin("carol_fun");
        String dave = registerAndLogin("dave_fun");

        Response created = client.createList(carol, "Zero Names");
        created.then().statusCode(201);
        int listId = created.jsonPath().getInt("id");
        String code = created.jsonPath().getString("invitationCode");
        client.joinList(dave, code).then().statusCode(200);

        // FR-9: exact English message
        client.finishAddition(carol, listId).then().statusCode(400)
                .body("error", org.hamcrest.Matchers.equalTo(
                        "At least one name must be provided to proceed to the selection phase."));
        client.finishAddition(dave, listId).then().statusCode(400);
        client.getActiveList(carol).then()
                .statusCode(200)
                .body("phase", org.hamcrest.Matchers.equalTo("ADDITION"));
    }

    @Test
    void additionPhaseExpiresAfter48Hours() {
        String owner = registerAndLogin("owner_time");
        String joiner = registerAndLogin("joiner_time");

        Response created = client.createList(owner, "Timeout List");
        created.then().statusCode(201);
        int listId = created.jsonPath().getInt("id");
        String code = created.jsonPath().getString("invitationCode");
        client.joinList(joiner, code).then().statusCode(200);
        client.getActiveList(owner).then().statusCode(200).body("phase", org.hamcrest.Matchers.equalTo("ADDITION"));

        // FR-8: age the list beyond 48 hours and run the scheduler tick
        ListEntity entity = listRepository.findById((long) listId).orElseThrow();
        entity.setCreatedAt(Instant.now().minus(49, ChronoUnit.HOURS));
        listRepository.save(entity);
        phaseTransitionService.expireStaleAdditionLists();

        // Terminal EXPIRED is no longer an active list
        client.getActiveList(owner).then().statusCode(404);
        assertThat(listRepository.findById((long) listId).orElseThrow().getPhase()).isEqualTo(ListPhase.EXPIRED);
    }
}