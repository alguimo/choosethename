package com.choosethename.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "list_memberships")
@Getter
@Setter
public class ListMembershipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "list_id")
    private Long listId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "joined_at")
    private Instant joinedAt;
}
