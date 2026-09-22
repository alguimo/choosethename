package com.choosethename.backend.repository;

import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListPhase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListRepository extends JpaRepository<ListEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ListEntity l WHERE l.invitationCode = :code")
    Optional<ListEntity> findByInvitationCodeForUpdate(@Param("code") String code);

    @Query("SELECT l FROM ListEntity l WHERE l.id IN " +
           "(SELECT m.listId FROM ListMembershipEntity m WHERE m.userId = :userId) " +
           "AND l.phase <> :excludedPhase " +
           "ORDER BY l.createdAt DESC")
    List<ListEntity> findListsForUser(@Param("userId") Long userId, @Param("excludedPhase") ListPhase excludedPhase);

    List<ListEntity> findByPhaseAndCreatedAtBefore(ListPhase phase, Instant createdAt);
}