package com.choosethename.backend.service;

import org.springframework.stereotype.Component;
import java.text.Normalizer;

@Component
public class NameNormalizer {

    public String normalize(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String trimmed = name.strip();
        String collapsed = trimmed.replaceAll("\\s+", " ");
        String normalized = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }
}
