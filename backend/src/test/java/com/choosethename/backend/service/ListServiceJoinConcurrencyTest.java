package com.choosethename.backend.service;

import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListMembershipEntity;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.Role;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ListServiceJoinConcurrencyTest {

    private static final String CODE = "JOIN01";
    private static final int MAX_MEMBERS = 5;

    @Autowired private ListService listService;
    @Autowired private ListRepository listRepository;
    @Autowired private ListMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private ListEntity list;
    private User owner;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM votes");
        jdbcTemplate.execute("DELETE FROM voting_rounds");
        jdbcTemplate.execute("DELETE FROM shared_name_pool");
        jdbcTemplate.execute("DELETE FROM names");
        jdbcTemplate.execute("DELETE FROM list_memberships");
        jdbcTemplate.execute("DELETE FROM lists");
        jdbcTemplate.execute("DELETE FROM users");

        owner = createUser("owner");
        list = new ListEntity();
        list.setName("Concurrent List");
        list.setOwnerId(owner.getId());
        list.setInvitationCode(CODE);
        list.setPhase(ListPhase.ADDITION);
        list.setInvitationsOpen(true);
        list.setCodeExpiresAt(Instant.now().plusSeconds(48 * 3600));
        list.setCurrentRound(1);
        list.setTotalRounds(1);
        list.setCreatedAt(Instant.now());
        list = listRepository.save(list);

        addMembership(list.getId(), owner.getId());
        for (int i = 0; i < 3; i++) {
            addMembership(list.getId(), createUser("member" + i).getId());
        }
    }

    @Test
    @DisplayName("NFR: concurrent joins never exceed the 5-member cap")
    void concurrentJoinsNeverExceedMemberCap() throws Exception {
        List<User> joiners = new ArrayList<>();
        for (int i = 0; i < MAX_MEMBERS; i++) {
            joiners.add(createUser("joiner" + i));
        }

        ExecutorService executor = Executors.newFixedThreadPool(joiners.size());
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        try {
            var futures = joiners.stream()
                    .map(joiner -> executor.submit(() -> runJoin(joiner.getId(), successes, rejections)))
                    .toList();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes.get()).isEqualTo(1);
        assertThat(rejections.get()).isEqualTo(MAX_MEMBERS - 1);
        assertThat(membershipRepository.countByListId(list.getId())).isEqualTo(MAX_MEMBERS);
        assertThat(listRepository.findById(list.getId()).orElseThrow().isInvitationsOpen()).isFalse();
    }

    private void runJoin(Long userId, AtomicInteger successes, AtomicInteger rejections) {
        try {
            listService.joinList(userId, CODE);
            successes.incrementAndGet();
        } catch (ListOperationException e) {
            rejections.incrementAndGet();
        }
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + System.nanoTime());
        user.setPasswordHash("password");
        user.setRole(Role.PARTICIPANT);
        return userRepository.save(user);
    }

    private void addMembership(Long listId, Long userId) {
        ListMembershipEntity membership = new ListMembershipEntity();
        membership.setListId(listId);
        membership.setUserId(userId);
        membership.setJoinedAt(Instant.now());
        membershipRepository.save(membership);
    }
}