package com.choosethename.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResultsResponseDTO {
    private List<ResultEntry> results;

    @Getter
    @Setter
    public static class ResultEntry {
        private Integer rank;
        private String name;
        private int score;

        public ResultEntry() {
        }

        public ResultEntry(Integer rank, String name, int score) {
            this.rank = rank;
            this.name = name;
            this.score = score;
        }
    }
}