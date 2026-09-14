package com.choosethename.backend.dto;

import com.choosethename.backend.model.ListEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListMapperTest {

    private final ListMapper mapper = Mappers.getMapper(ListMapper.class);

    @Test
    @DisplayName("NFR-2: Should map ListEntity to ListResponseDTO correctly")
    void shouldMapListEntityToDto() {
        ListEntity entity = new ListEntity();
        entity.setId(1L);
        entity.setName("Test List");
        entity.setInvitationCode("CODE01");
        entity.setCodeExpiresAt(Instant.now().plusSeconds(3600));
        entity.setPhase("ADDITION");
        entity.setInvitationsOpen(true);
        entity.setOwnerId(10L);

        ListResponseDTO dto = mapper.toResponseDTO(entity, List.of("user1", "user2"), "owner1");

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Test List");
        assertThat(dto.getInvitationCode()).isEqualTo("CODE01");
        assertThat(dto.getCodeExpiresAt()).isEqualTo(entity.getCodeExpiresAt());
        assertThat(dto.getPhase()).isEqualTo("ADDITION");
        assertThat(dto.isInvitationsOpen()).isTrue();
        assertThat(dto.getOwnerUsername()).isEqualTo("owner1");
        assertThat(dto.getMembers()).containsExactly("user1", "user2");
    }
}