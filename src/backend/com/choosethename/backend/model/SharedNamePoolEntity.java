package com.choosethename.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "shared_name_pool")
@Getter
@Setter
public class SharedNamePoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "normalized_name")
    private String normalizedName;

    @Column(name = "adopted_by")
    private Long adoptedBy;

    @Column(name = "adopted_at")
    private Instant adoptedAt;
}
