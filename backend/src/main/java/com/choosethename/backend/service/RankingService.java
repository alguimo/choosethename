package com.choosethename.backend.service;

import com.choosethename.backend.dto.VoteJsonCodec;
import com.choosethename.backend.model.VoteEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RankingService {

    public static final int LARGE_POOL_THRESHOLD = 15;
    public static final int FINAL_RESULT_SIZE = 3;

    public record ScoredName(String name, int score) {
    }

    public int totalRoundsFor(int initialPoolSize) {
        return initialPoolSize > LARGE_POOL_THRESHOLD ? 3 : 2;
    }

    public int capForRound(int roundNumber, int totalRounds) {
        return switch (totalRounds) {
            case 3 -> switch (roundNumber) {
                case 1 -> 10;
                case 2 -> 5;
                case 3 -> FINAL_RESULT_SIZE;
                default -> throw new IllegalArgumentException("Unexpected round number: " + roundNumber);
            };
            case 2 -> switch (roundNumber) {
                case 1 -> 5;
                case 2 -> FINAL_RESULT_SIZE;
                default -> throw new IllegalArgumentException("Unexpected round number: " + roundNumber);
            };
            default -> throw new IllegalArgumentException("Unexpected total rounds: " + totalRounds);
        };
    }

    public boolean isLastRound(int roundNumber, int initialPoolSize) {
        return roundNumber == totalRoundsFor(initialPoolSize);
    }

    public List<ScoredName> scoreRound(List<String> pool, List<VoteEntity> votes) {
        int poolSize = pool.size();
        Map<String, Integer> scores = new HashMap<>();
        for (String name : pool) {
            scores.put(name, 0);
        }

        for (VoteEntity vote : votes) {
            List<String> ranking = VoteJsonCodec.decode(vote.getRankings());
            for (int i = 0; i < ranking.size(); i++) {
                String name = ranking.get(i);
                if (scores.containsKey(name)) {
                    scores.merge(name, poolSize - (i + 1), Integer::sum);
                }
            }
        }

        return pool.stream()
                .map(name -> new ScoredName(name, scores.getOrDefault(name, 0)))
                .sorted(Comparator.comparingInt(ScoredName::score).reversed()
                        .thenComparing(ScoredName::name))
                .toList();
    }

    public List<String> survivors(List<ScoredName> ranked, int cap) {
        int keep = Math.min(cap, ranked.size());
        return ranked.stream()
                .limit(keep)
                .map(ScoredName::name)
                .toList();
    }
}