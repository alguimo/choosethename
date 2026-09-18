package com.choosethename.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VoteRequestDTO {
    private Integer roundNumber;
    private List<String> rankings;
}