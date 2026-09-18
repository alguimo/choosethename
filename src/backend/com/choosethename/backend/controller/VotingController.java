package com.choosethename.backend.controller;

import com.choosethename.backend.dto.ResultsResponseDTO;
import com.choosethename.backend.dto.VoteRequestDTO;
import com.choosethename.backend.service.CurrentUserResolver;
import com.choosethename.backend.service.VotingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lists")
public class VotingController {

    private final VotingService votingService;
    private final CurrentUserResolver currentUserResolver;

    public VotingController(VotingService votingService, CurrentUserResolver currentUserResolver) {
        this.votingService = votingService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<Void> submitVote(
            @PathVariable Long id,
            @RequestBody VoteRequestDTO request,
            @AuthenticationPrincipal String username) {
        Long userId = currentUserResolver.requireUserId(username);
        votingService.submitVote(id, userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ResultsResponseDTO> getResults(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        Long userId = currentUserResolver.requireUserId(username);
        return ResponseEntity.ok(votingService.getResults(id, userId));
    }
}