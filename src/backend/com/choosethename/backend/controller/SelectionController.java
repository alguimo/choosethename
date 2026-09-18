package com.choosethename.backend.controller;

import com.choosethename.backend.dto.AdoptNameRequestDTO;
import com.choosethename.backend.dto.SelectionResponseDTO;
import com.choosethename.backend.service.CurrentUserResolver;
import com.choosethename.backend.service.SelectionService;
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
public class SelectionController {

    private final SelectionService selectionService;
    private final CurrentUserResolver currentUserResolver;

    public SelectionController(SelectionService selectionService, CurrentUserResolver currentUserResolver) {
        this.selectionService = selectionService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/{id}/selection")
    public ResponseEntity<SelectionResponseDTO> getSelection(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        Long userId = currentUserResolver.requireUserId(username);
        return ResponseEntity.ok(selectionService.getSelection(id, userId));
    }

    @PostMapping("/{id}/selection/adopt")
    public ResponseEntity<Void> adoptFadedName(
            @PathVariable Long id,
            @RequestBody AdoptNameRequestDTO request,
            @AuthenticationPrincipal String username) {
        Long userId = currentUserResolver.requireUserId(username);
        selectionService.adoptFadedName(id, userId, request);
        return ResponseEntity.ok().build();
    }
}