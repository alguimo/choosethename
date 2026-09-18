package com.choosethename.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameNormalizerTest {

    private final NameNormalizer normalizer = new NameNormalizer();

    @Test
    @DisplayName("Should trim leading and trailing whitespace")
    void shouldTrimWhitespace() {
        assertThat(normalizer.normalize("  hello  ")).isEqualTo("hello");
    }

    @Test
    @DisplayName("Should collapse consecutive whitespace into a single space")
    void shouldCollapseConsecutiveWhitespace() {
        assertThat(normalizer.normalize("hello   world")).isEqualTo("hello world");
    }

    @Test
    @DisplayName("Should strip accents and convert to lowercase")
    void shouldStripAccentsAndLowercase() {
        assertThat(normalizer.normalize("José")).isEqualTo("jose");
        assertThat(normalizer.normalize("café")).isEqualTo("cafe");
        assertThat(normalizer.normalize("PABLO García")).isEqualTo("pablo garcia");
    }

    @Test
    @DisplayName("Should handle combined normalization: trim, collapse, strip accents, lowercase")
    void shouldHandleCombinedNormalization() {
        assertThat(normalizer.normalize("  PABLO  García  ")).isEqualTo("pablo garcia");
        assertThat(normalizer.normalize("  María  José  López  ")).isEqualTo("maria jose lopez");
    }

    @Test
    @DisplayName("Should return empty string when input is only whitespace")
    void shouldReturnEmptyForWhitespaceOnly() {
        assertThat(normalizer.normalize("   ")).isEmpty();
        assertThat(normalizer.normalize("     ")).isEmpty();
        assertThat(normalizer.normalize("\t\n")).isEmpty();
    }

    @Test
    @DisplayName("Should collapse tabs and newlines into a single space")
    void shouldCollapseTabsAndNewlines() {
        assertThat(normalizer.normalize("hello\tworld")).isEqualTo("hello world");
        assertThat(normalizer.normalize("hello\n\nworld")).isEqualTo("hello world");
    }

    @Test
    @DisplayName("Should return same string when already normalized")
    void shouldReturnSameWhenAlreadyNormalized() {
        assertThat(normalizer.normalize("hello")).isEqualTo("hello");
    }

    @Test
    @DisplayName("Should handle multiple complex spaces between words")
    void shouldHandleMultipleComplexSpaces() {
        assertThat(normalizer.normalize("hello   world   foo")).isEqualTo("hello world foo");
    }

    @Test
    @DisplayName("Should handle empty string input")
    void shouldHandleEmptyStringInput() {
        assertThat(normalizer.normalize("")).isEmpty();
    }
}
