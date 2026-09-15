package com.choosethename.backend.service;

import com.choosethename.backend.model.VoteEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServiceTest {

    private final RankingService rankingService = new RankingService();

    @Nested
    @DisplayName("Round structure")
    class RoundStructure {

        @Test
        @DisplayName("FR-4: more than 15 names require 3 rounds with caps 10/5/3")
        void largePoolUsesThreeRounds() {
            assertThat(rankingService.totalRoundsFor(16)).isEqualTo(3);
            assertThat(rankingService.capForRound(1, 3)).isEqualTo(10);
            assertThat(rankingService.capForRound(2, 3)).isEqualTo(5);
            assertThat(rankingService.capForRound(3, 3)).isEqualTo(3);
            assertThat(rankingService.isLastRound(3, 16)).isTrue();
            assertThat(rankingService.isLastRound(2, 16)).isFalse();
        }

        @Test
        @DisplayName("FR-5: 15 or fewer names require 2 rounds with caps 5/3")
        void smallPoolUsesTwoRounds() {
            assertThat(rankingService.totalRoundsFor(15)).isEqualTo(2);
            assertThat(rankingService.totalRoundsFor(3)).isEqualTo(2);
            assertThat(rankingService.capForRound(1, 2)).isEqualTo(5);
            assertThat(rankingService.capForRound(2, 2)).isEqualTo(3);
            assertThat(rankingService.isLastRound(2, 15)).isTrue();
            assertThat(rankingService.isLastRound(1, 15)).isFalse();
        }
    }

    @Nested
    @DisplayName("Borda scoring")
    class BordaScoring {

        @Test
        @DisplayName("single voter assigns poolSize - rankPosition points")
        void singleVoterBordaPoints() {
            List<String> pool = List.of("ana", "luis", "pablo");
            List<RankingService.ScoredName> ranked = rankingService.scoreRound(pool, List.of(vote("ana", "luis", "pablo")));

            assertThat(scoreOf(ranked, "ana")).isEqualTo(2);
            assertThat(scoreOf(ranked, "luis")).isEqualTo(1);
            assertThat(scoreOf(ranked, "pablo")).isEqualTo(0);
        }

        @Test
        @DisplayName("consolidated score is the sum over all voters")
        void consolidatedScoreIsSumOverVoters() {
            List<String> pool = List.of("ana", "luis", "pablo");
            List<VoteEntity> votes = List.of(
                    vote("ana", "luis", "pablo"),
                    vote("pablo", "luis", "ana")
            );

            List<RankingService.ScoredName> ranked = rankingService.scoreRound(pool, votes);

            assertThat(scoreOf(ranked, "ana")).isEqualTo(2 + 0);
            assertThat(scoreOf(ranked, "luis")).isEqualTo(1 + 1);
            assertThat(scoreOf(ranked, "pablo")).isEqualTo(0 + 2);
        }

        @Test
        @DisplayName("missing names in a ranking contribute zero points")
        void missingNamesContributeZero() {
            List<String> pool = List.of("ana", "luis", "pablo");
            List<RankingService.ScoredName> ranked = rankingService.scoreRound(pool, List.of(vote("ana")));

            assertThat(scoreOf(ranked, "ana")).isEqualTo(2);
            assertThat(scoreOf(ranked, "luis")).isZero();
            assertThat(scoreOf(ranked, "pablo")).isZero();
        }
    }

    @Nested
    @DisplayName("Ordering and tie-breaks")
    class Ordering {

        @Test
        @DisplayName("NFR-2: ties are broken alphabetically by normalized name")
        void tiesBrokenAlphabetically() {
            List<String> pool = List.of("beta", "alpha");
            List<VoteEntity> votes = List.of(
                    vote("alpha", "beta"),
                    vote("beta", "alpha")
            );

            List<RankingService.ScoredName> ranked = rankingService.scoreRound(pool, votes);

            assertThat(ranked.stream().map(RankingService.ScoredName::name).toList())
                    .containsExactly("alpha", "beta");
        }

        @Test
        @DisplayName("NFR-2: final positions 1-2-3 resolve ties alphabetically")
        void topThreePositionsResolveTiesAlphabetically() {
            List<String> pool = List.of("zeta", "beta", "gamma", "alpha", "delta");
            // Everyone ranks the same order: delta, beta, gamma, zeta, alpha
            List<VoteEntity> votes = List.of(
                    vote("delta", "beta", "gamma", "zeta", "alpha"),
                    vote("delta", "beta", "gamma", "zeta", "alpha")
            );

            List<RankingService.ScoredName> ranked = rankingService.scoreRound(pool, votes);

            assertThat(ranked.stream().map(RankingService.ScoredName::name).toList())
                    .containsExactly("delta", "beta", "gamma", "zeta", "alpha");
        }
    }

    @Nested
    @DisplayName("Elimination")
    class Elimination {

        @Test
        @DisplayName("eliminate keeps the top cap names in order")
        void eliminateKeepsTopCap() {
            List<RankingService.ScoredName> ranked = List.of(
                    new RankingService.ScoredName("ana", 9),
                    new RankingService.ScoredName("luis", 7),
                    new RankingService.ScoredName("pablo", 5),
                    new RankingService.ScoredName("maria", 3)
            );

            assertThat(rankingService.survivors(ranked, 3)).containsExactly("ana", "luis", "pablo");
        }

        @Test
        @DisplayName("edge case: pool smaller than the cap keeps all names")
        void degradeCapWhenPoolSmaller() {
            List<RankingService.ScoredName> ranked = List.of(
                    new RankingService.ScoredName("ana", 9),
                    new RankingService.ScoredName("luis", 7)
            );

            assertThat(rankingService.survivors(ranked, 10)).containsExactly("ana", "luis");
            assertThat(rankingService.survivors(ranked, 3)).containsExactly("ana", "luis");
        }

        @Test
        @DisplayName("edge case: fewer than 3 names are kept entirely")
        void fewerThanThreeNamesKeptEntirely() {
            List<RankingService.ScoredName> ranked = List.of(
                    new RankingService.ScoredName("ana", 4),
                    new RankingService.ScoredName("luis", 2)
            );

            assertThat(rankingService.survivors(ranked, 3)).containsExactly("ana", "luis");
        }

        @Test
        @DisplayName("NFR-2: ties at the elimination threshold are resolved alphabetically")
        void tieAtEliminationThresholdResolvedAlphabetically() {
            List<String> pool = List.of("zeta", "beta", "alpha", "ana");
            List<VoteEntity> votes = List.of(
                    vote("ana", "alpha", "beta", "zeta"),
                    vote("ana", "beta", "alpha", "zeta")
            );

            // ana: 6, alpha: 3, beta: 3, zeta: 0. Alpha beats beta alphabetically at the tie.
            List<RankingService.ScoredName> ranked = rankingService.scoreRound(pool, votes);
            assertThat(rankingService.survivors(ranked, 3)).containsExactly("ana", "alpha", "beta");
        }
    }

    private VoteEntity vote(String... rankings) {
        VoteEntity entity = new VoteEntity();
        entity.setRankings(com.choosethename.backend.dto.VoteJsonCodec.encode(List.of(rankings)));
        entity.setSubmittedAt(Instant.now());
        return entity;
    }

    private int scoreOf(List<RankingService.ScoredName> ranked, String name) {
        return ranked.stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow()
                .score();
    }
}