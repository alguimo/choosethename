package com.choosethename.backend.dto;

import com.choosethename.backend.model.ListEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ListMapper {
    @Mapping(target = "members", source = "members")
    @Mapping(target = "currentRound", source = "entity.currentRound")
    @Mapping(target = "totalRounds", source = "entity.totalRounds")
    @Mapping(target = "currentPool", source = "currentPool")
    @Mapping(target = "myStepCompleted", source = "myStepCompleted")
    ListResponseDTO toResponseDTO(ListEntity entity, List<String> members, String ownerUsername,
                                  List<String> currentPool, boolean myStepCompleted);
}