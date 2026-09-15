package com.choosethename.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "votes")
@Getter
@Setter
public class VoteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "round_id")
    private Long roundId;

    @Column(name = "rankings")
    private String rankings;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}