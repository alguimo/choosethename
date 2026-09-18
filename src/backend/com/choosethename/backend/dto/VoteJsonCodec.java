package com.choosethename.backend.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class VoteJsonCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private VoteJsonCodec() {
    }

    public static String encode(List<String> names) {
        try {
            return OBJECT_MAPPER.writeValueAsString(names);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize rankings", e);
        }
    }

    public static List<String> decode(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialize rankings", e);
        }
    }
}