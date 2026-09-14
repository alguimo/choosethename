package com.choosethename.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InvitationCodeGeneratorTest {

    private final InvitationCodeGenerator generator = new InvitationCodeGenerator();

    @Test
    @DisplayName("Should generate a 6-character alphanumeric code")
    void shouldGenerateCode() {
        String code = generator.generate();
        assertThat(code).hasSize(6);
        assertThat(code).matches("^[A-Z0-9]+$");
    }
}
