package com.choosethename.backend.functional;

import com.choosethename.backend.model.ListEntity;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class Spec002FunctionalTest extends FunctionalTestBase {

    @Test
    void createListJourney() {
        String alvaro = registerAndLogin("alvaro_fun");
        String maria = registerAndLogin("maria_fun");
        String carlos = registerAndLogin("carlos_fun");
        String david = registerAndLogin("david_fun");
        String elena = registerAndLogin("elena_fun");
        String francisco = registerAndLogin("francisco_fun");
        String gema = registerAndLogin("gema_fun");

        // TS-1: Create list successfully
        Response created = client.createList(alvaro, "Baby Names 2026");
        created.then().statusCode(201);
        String code = created.jsonPath().getString("invitationCode");
        int listId = created.jsonPath().getInt("id");
        assertThat(code).hasSize(6).matches("^[A-Z0-9]{6}$");
        assertThat(Instant.parse(created.jsonPath().getString("codeExpiresAt")))
                .isAfter(Instant.now().plusSeconds(47L * 3600));
        assertThat(created.jsonPath().getString("phase")).isEqualTo("ADDITION");
        assertThat(created.jsonPath().getBoolean("invitationsOpen")).isTrue();
        assertThat(created.jsonPath().getString("ownerUsername")).isEqualTo("alvaro_fun");
        assertThat(created.jsonPath().getList("members")).containsExactly("alvaro_fun");

        // TS-2: create another list while already belonging to one => 201 (multi-list)
        Response second = client.createList(alvaro, "Second List");
        second.then().statusCode(201);
        int secondId = second.jsonPath().getInt("id");

        // TS-3: blank name => 400
        client.createList(maria, "   ").then().statusCode(400);
        client.createList(maria, "").then().statusCode(400);

        // TS-5: malformed code => 400
        client.joinList(maria, "AB12").then().statusCode(400);
        client.joinList(maria, "ABCDEFG").then().statusCode(400);

        // TS-6: non-existent code => 404
        client.joinList(maria, "NOPE99").then().statusCode(404);

        // TS-4: valid join (case-insensitive) => 200, member added
        Response joined = client.joinList(maria, code.toLowerCase());
        joined.then().statusCode(200);
        assertThat(joined.jsonPath().getBoolean("invitationsOpen")).isTrue();
        assertThat(joined.jsonPath().getList("members")).contains("alvaro_fun", "maria_fun");

        // TS-10: already a member => 400
        client.joinList(maria, code).then().statusCode(400);

        // TS-9/TS-8: 3rd + 4th join ok, 5th auto-closes, 6th rejected
        client.joinList(carlos, code).then().statusCode(200);
        client.joinList(david, code).then().statusCode(200);
        Response fifth = client.joinList(elena, code);
        fifth.then().statusCode(200);
        assertThat(fifth.jsonPath().getBoolean("invitationsOpen")).isFalse();
        client.joinList(francisco, code).then().statusCode(400);
        client.joinList(gema, code).then().statusCode(400);

        // TS-11: owner closes invitations
        Response closed = client.closeInvitations(alvaro, listId);
        closed.then().statusCode(200);
        assertThat(closed.jsonPath().getBoolean("invitationsOpen")).isFalse();

        // TS-12: non-owner cannot close invitations => 403
        client.closeInvitations(maria, listId).then().statusCode(403);

        // TS-15: fetch a list by id as a member with full details
        Response detail = client.getListById(alvaro, listId);
        detail.then().statusCode(200);
        assertThat(detail.jsonPath().getInt("id")).isEqualTo(listId);
        assertThat(detail.jsonPath().getString("name")).isEqualTo("Baby Names 2026");
        assertThat(detail.jsonPath().getString("phase")).isEqualTo("ADDITION");
        assertThat(detail.jsonPath().getString("invitationCode")).isEqualTo(code);
        assertThat(detail.jsonPath().getList("members"))
                .containsExactlyInAnyOrder("alvaro_fun", "maria_fun", "carlos_fun", "david_fun", "elena_fun");

        // TS-14: my lists include every list the user belongs to (ordered by creation desc)
        Response myLists = client.getMyLists(alvaro);
        myLists.then().statusCode(200);
        assertThat(myLists.jsonPath().getList("id")).contains(listId, secondId);

        // TS-13: user who belongs to no list => 200 with an empty array
        client.getMyLists(gema).then().statusCode(200).body("$", org.hamcrest.Matchers.empty());

        // TS-16: fetch as a non-member => 404
        client.getListById(gema, listId).then().statusCode(404);

        // TS-17: fetch a non-existent list => 404
        client.getListById(alvaro, 999_999).then().statusCode(404);
    }

    @Test
    void joinWithExpiredCodeIsRejected() {
        String owner = registerAndLogin("owner_exp");
        String joiner = registerAndLogin("joiner_exp");

        Response created = client.createList(owner, "Expiring List");
        created.then().statusCode(201);
        String code = created.jsonPath().getString("invitationCode");
        int listId = created.jsonPath().getInt("id");

        ListEntity entity = listRepository.findById((long) listId).orElseThrow();
        entity.setCodeExpiresAt(Instant.now().minusSeconds(60));
        listRepository.save(entity);

        // TS-7: expired code => 400
        client.joinList(joiner, code).then().statusCode(400);
    }
}