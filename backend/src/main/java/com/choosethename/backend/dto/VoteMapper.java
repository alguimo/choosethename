package com.choosethename.backend.dto;

import com.choosethename.backend.model.VoteEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VoteMapper {

    VoteEntity toEntity(VoteRequestDTO request, Long listId, Long userId, Long roundId);

    default String mapRankings(List<String> rankings) {
        return VoteJsonCodec.encode(rankings);
    }
}