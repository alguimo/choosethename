package com.choosethename.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NameResponseDTO {
    private List<NameEntry> names;

    @Getter
    @Setter
    public static class NameEntry {
        private String name;
        private String normalizedName;

        public NameEntry() {
        }

        public NameEntry(String name, String normalizedName) {
            this.name = name;
            this.normalizedName = normalizedName;
        }
    }
}