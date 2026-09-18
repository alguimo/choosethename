package com.choosethename.backend.dto;

import com.choosethename.backend.model.VoteEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VoteMapperTest {

    private final VoteMapper mapper = Mappers.getMapper(VoteMapper.class);

    @Test
    @DisplayName("Should map VoteRequestDTO to VoteEntity with serialized rankings")
    void shouldMapRequestToEntity() {
        VoteRequestDTO request = new VoteRequestDTO();
        request.setRankings(List.of("ana", "luis", "pablo"));

        VoteEntity entity = mapper.toEntity(request, 1L, 10L, 100L);

        assertThat(entity.getListId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(10L);
        assertThat(entity.getRoundId()).isEqualTo(100L);
        assertThat(entity.getRankings()).isEqualTo("[\"ana\",\"luis\",\"pablo\"]");
    }

    @Test
    @DisplayName("Should serialize single-element rankings to a JSON array")
    void shouldSerializeSingleElementRankings() {
        VoteRequestDTO request = new VoteRequestDTO();
        request.setRankings(List.of("solo"));

        VoteEntity entity = mapper.toEntity(request, 1L, 10L, 100L);

        assertThat(entity.getRankings()).isEqualTo("[\"solo\"]");
    }
}