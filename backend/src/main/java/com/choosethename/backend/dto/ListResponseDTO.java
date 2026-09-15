package com.choosethename.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ListResponseDTO {
    private Long id;
    private String name;
    private String invitationCode;
    private Instant codeExpiresAt;
    private String phase;
    private boolean invitationsOpen;
    private String ownerUsername;
    private int currentRound;
    private int totalRounds;
    private List<String> currentPool;
    private List<String> members;
}
