package com.choosethename.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "lists")
@Getter
@Setter
public class ListEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "invitation_code")
    private String invitationCode;
    @Column(name = "code_expires_at")
    private Instant codeExpiresAt;
    private String phase;
    @Column(name = "invitations_open")
    private boolean invitationsOpen;
    @Column(name = "owner_id")
    private Long ownerId;
    @Version
    private int version;
    @Column(name = "created_at")
    private Instant createdAt;
}
