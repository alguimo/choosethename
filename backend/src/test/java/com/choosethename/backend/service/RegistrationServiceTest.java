package com.choosethename.backend.service;

import com.choosethename.backend.exception.UserAlreadyExistsException;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        registrationService = new RegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterParticipantSuccessfully() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User user = registrationService.registerParticipant("user1", "Password123");

        assertThat(user.getUsername()).isEqualTo("user1");
        assertThat(passwordEncoder.matches("Password123", user.getPasswordHash())).isTrue();
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> registrationService.registerParticipant("user1", "Password123"))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void shouldRejectWeakPassword() {
        assertThatThrownBy(() -> registrationService.registerParticipant("user1", "short"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
