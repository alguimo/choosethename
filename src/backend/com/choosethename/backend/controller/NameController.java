package com.choosethename.backend.controller;

import com.choosethename.backend.dto.AddNameRequestDTO;
import com.choosethename.backend.dto.NameResponseDTO;
import com.choosethename.backend.service.CurrentUserResolver;
import com.choosethename.backend.service.ListPhaseTransitionService;
import com.choosethename.backend.service.NameService;
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
public class NameController {

    private final NameService nameService;
    private final ListPhaseTransitionService phaseTransitionService;
    private final CurrentUserResolver currentUserResolver;

    public NameController(NameService nameService, ListPhaseTransitionService phaseTransitionService,
                          CurrentUserResolver currentUserResolver) {
        this.nameService = nameService;
        this.phaseTransitionService = phaseTransitionService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/{id}/names")
    public ResponseEntity<NameResponseDTO> addNames(
            @PathVariable Long id,
            @RequestBody AddNameRequestDTO request,
            @AuthenticationPrincipal String username) {
        Long userId = currentUserResolver.requireUserId(username);
        return ResponseEntity.ok(nameService.addNames(id, userId, request));
    }

    @GetMapping("/{id}/names")
    public ResponseEntity<NameResponseDTO> getNames(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        Long userId = currentUserResolver.requireUserId(username);
        return ResponseEntity.ok(nameService.getNames(id, userId));
    }

    @PostMapping("/{id}/finish-addition")
    public ResponseEntity<Void> finishAddition(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        Long userId = currentUserResolver.requireUserId(username);
        nameService.finishAddition(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete-selection")
    public ResponseEntity<Void> completeSelection(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        Long userId = currentUserResolver.requireUserId(username);
        phaseTransitionService.completeSelection(id, userId);
        return ResponseEntity.ok().build();
    }
}