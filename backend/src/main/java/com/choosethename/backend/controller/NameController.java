package com.choosethename.backend.controller;

import com.choosethename.backend.dto.AddNameRequestDTO;
import com.choosethename.backend.dto.NameResponseDTO;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.service.ListPhaseTransitionService;
import com.choosethename.backend.service.NameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final UserRepository userRepository;

    public NameController(NameService nameService, ListPhaseTransitionService phaseTransitionService,
                          UserRepository userRepository) {
        this.nameService = nameService;
        this.phaseTransitionService = phaseTransitionService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{id}/names")
    public ResponseEntity<NameResponseDTO> addNames(
            @PathVariable Long id,
            @RequestBody AddNameRequestDTO request,
            @AuthenticationPrincipal String username) {
        Long userId = requireUserId(username);
        return ResponseEntity.ok(nameService.addNames(id, userId, request));
    }

    @PostMapping("/{id}/finish-addition")
    public ResponseEntity<Void> finishAddition(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        Long userId = requireUserId(username);
        nameService.finishAddition(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete-selection")
    public ResponseEntity<Void> completeSelection(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        Long userId = requireUserId(username);
        phaseTransitionService.completeSelection(id, userId);
        return ResponseEntity.ok().build();
    }

    private Long requireUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ListNotFoundException("User not found"))
                .getId();
    }
}