package com.choosethename.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SelectionResponseDTO {
    private List<NameResponseDTO.NameEntry> commonNames;
    private List<NameResponseDTO.NameEntry> fadedSuggestions;
    private List<NameResponseDTO.NameEntry> myNames;
}