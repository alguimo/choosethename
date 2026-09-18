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
@Table(name = "voting_rounds")
@Getter
@Setter
public class VotingRoundEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "pool_rankings")
    private String poolRankings;

    @Column(name = "created_at")
    private Instant createdAt;
}