package com.choosethename.backend.service;

import com.choosethename.backend.dto.VoteJsonCodec;
import com.choosethename.backend.dto.VoteMapper;
import com.choosethename.backend.dto.VoteRequestDTO;
import com.choosethename.backend.exception.DuplicateNameException;
import com.choosethename.backend.exception.ListAccessDeniedException;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.exception.ResultsNotReadyException;
import com.choosethename.backend.exception.StaleVoteException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.VoteEntity;
import com.choosethename.backend.model.VotingRoundEntity;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.VoteRepository;
import com.choosethename.backend.repository.VotingRoundRepository;
import com.choosethename.backend.dto.ResultsResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VotingService {

    private final ListRepository listRepository;
    private final ListMembershipRepository membershipRepository;
    private final VotingRoundRepository votingRoundRepository;
    private final VoteRepository voteRepository;
    private final RankingService rankingService;
    private final VoteMapper voteMapper;
    private final NamePoolService namePoolService;

    @Transactional
    public void submitVote(Long listId, Long userId, VoteRequestDTO request) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (list.getPhase() != ListPhase.VOTING) {
            throw new ListOperationException("Votes can only be submitted during the VOTING phase");
        }
        if (membershipRepository.findByListIdAndUserId(listId, userId).isEmpty()) {
            throw new ListAccessDeniedException("User is not a member of this list");
        }
        if (request == null || request.getRoundNumber() == null) {
            throw new ListOperationException("Round number is required");
        }
        if (request.getRoundNumber() < list.getCurrentRound()) {
            throw new StaleVoteException("This voting round has already been finalized");
        }
        if (request.getRoundNumber() > list.getCurrentRound()) {
            throw new ListOperationException("Cannot vote on a future round");
        }

        VotingRoundEntity round = votingRoundRepository
                .findByListIdAndRoundNumber(listId, list.getCurrentRound())
                .orElseGet(() -> initializeRound(list));
        List<String> pool = VoteJsonCodec.decode(round.getPoolRankings());
        validateRankings(request.getRankings(), pool);

        VoteEntity existing = voteRepository.findByListIdAndUserIdAndRoundId(listId, userId, round.getId())
                .orElse(null);
        VoteEntity vote;
        if (existing == null) {
            vote = voteMapper.toEntity(request, listId, userId, round.getId());
        } else {
            existing.setRankings(VoteJsonCodec.encode(request.getRankings()));
            vote = existing;
        }
        vote.setSubmittedAt(Instant.now());
        voteRepository.save(vote);

        long votesCount = voteRepository.countByListIdAndRoundId(listId, round.getId());
        long membersCount = membershipRepository.countByListId(listId);
        if (votesCount >= membersCount) {
            consolidateAndAdvance(list, round, pool);
        }
    }

    private VotingRoundEntity initializeRound(ListEntity list) {
        List<String> pool = namePoolService.buildSharedPool(list.getId());
        VotingRoundEntity round = new VotingRoundEntity();
        round.setListId(list.getId());
        round.setRoundNumber(list.getCurrentRound());
        round.setPoolRankings(VoteJsonCodec.encode(pool));
        round.setCreatedAt(Instant.now());
        votingRoundRepository.save(round);
        if (list.getTotalRounds() == null || list.getTotalRounds() < 1) {
            list.setTotalRounds(rankingService.totalRoundsFor(pool.size()));
        }
        return round;
    }

    private void validateRankings(List<String> rankings, List<String> pool) {
        if (rankings == null || rankings.isEmpty()) {
            throw new ListOperationException("A full ranking of the current round's names is required");
        }
        Set<String> distinct = new HashSet<>(rankings);
        if (distinct.size() != rankings.size()) {
            throw new DuplicateNameException("Ranking contains duplicate names");
        }
        if (rankings.size() != pool.size() || !distinct.containsAll(pool)) {
            throw new ListOperationException("Ranking must include every name of the current round exactly once");
        }
    }

    private void consolidateAndAdvance(ListEntity list, VotingRoundEntity round, List<String> pool) {
        List<VoteEntity> votes = voteRepository.findByListIdAndRoundId(list.getId(), round.getId());
        List<RankingService.ScoredName> ranked = rankingService.scoreRound(pool, votes);

        int currentRound = list.getCurrentRound();
        int totalRounds = list.getTotalRounds();

        if (currentRound >= totalRounds) {
            list.setPhase(ListPhase.COMPLETED);
            listRepository.save(list);
            return;
        }

        int cap = rankingService.capForRound(currentRound, totalRounds);
        List<String> survivors = rankingService.survivors(ranked, cap);
        int nextRound = currentRound + 1;

        list.setCurrentRound(nextRound);
        listRepository.saveAndFlush(list);

        VotingRoundEntity next = new VotingRoundEntity();
        next.setListId(list.getId());
        next.setRoundNumber(nextRound);
        next.setPoolRankings(VoteJsonCodec.encode(survivors));
        next.setCreatedAt(Instant.now());
        votingRoundRepository.save(next);
    }

    @Transactional(readOnly = true)
    public ResultsResponseDTO getResults(Long listId, Long requestingUserId) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (membershipRepository.findByListIdAndUserId(listId, requestingUserId).isEmpty()) {
            throw new ListAccessDeniedException("User is not a member of this list");
        }
        if (list.getPhase() != ListPhase.COMPLETED) {
            throw new ResultsNotReadyException("Results are only available once the list is completed");
        }

        VotingRoundEntity finalRound = votingRoundRepository.findFirstByListIdOrderByRoundNumberDesc(listId)
                .orElseThrow(() -> new ListOperationException("Final voting round not found"));
        List<String> pool = VoteJsonCodec.decode(finalRound.getPoolRankings());
        List<VoteEntity> votes = voteRepository.findByListIdAndRoundId(listId, finalRound.getId());
        List<RankingService.ScoredName> ranked = rankingService.scoreRound(pool, votes);

        ResultsResponseDTO response = new ResultsResponseDTO();
        List<ResultsResponseDTO.ResultEntry> results = new ArrayList<>();
        int rank = 1;
        for (RankingService.ScoredName scored : ranked.stream().limit(3).toList()) {
            results.add(new ResultsResponseDTO.ResultEntry(rank++, scored.name(), scored.score()));
        }
        response.setResults(results);
        return response;
    }
}