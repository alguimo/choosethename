package com.choosethename.backend.controller;

import com.choosethename.backend.dto.CreateListRequestDTO;
import com.choosethename.backend.dto.JoinListRequestDTO;
import com.choosethename.backend.dto.ListResponseDTO;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.service.ListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lists")
public class ListController {

    private final ListService listService;
    private final UserRepository userRepository;

    public ListController(ListService listService, UserRepository userRepository) {
        this.listService = listService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ListResponseDTO> createList(
            @RequestBody CreateListRequestDTO request,
            @AuthenticationPrincipal String username) {
        Long userId = requireUserId(username);
        return ResponseEntity.status(HttpStatus.CREATED).body(listService.createList(request.getName(), userId));
    }

    @PostMapping("/join")
    public ResponseEntity<ListResponseDTO> joinList(
            @RequestBody JoinListRequestDTO request,
            @AuthenticationPrincipal String username) {
        Long userId = requireUserId(username);
        return ResponseEntity.ok(listService.joinList(userId, request.getCode()));
    }

    @GetMapping("/active")
    public ResponseEntity<ListResponseDTO> getActiveList(@AuthenticationPrincipal String username) {
        Long userId = requireUserId(username);
        return ResponseEntity.ok(listService.getActiveList(userId));
    }

    @PatchMapping("/{id}/close-invitations")
    public ResponseEntity<ListResponseDTO> closeInvitations(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        Long userId = requireUserId(username);
        return ResponseEntity.ok(listService.closeInvitations(id, userId));
    }

    private Long requireUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ListNotFoundException("User not found"))
                .getId();
    }
}