package com.choosethename.backend.dto;

import com.choosethename.backend.model.ListEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ListMapper {
    @Mapping(target = "members", source = "members")
    ListResponseDTO toResponseDTO(ListEntity entity, List<String> members, String ownerUsername);
}