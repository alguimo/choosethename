package com.choosethename.backend.service;

import com.choosethename.backend.dto.UserDTO;
import com.choosethename.backend.dto.UserMapper;
import com.choosethename.backend.exception.UserNotFoundException;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PasswordPolicy passwordPolicy;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        UserMapper userMapper, PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.passwordPolicy = passwordPolicy;
    }

    public List<UserDTO> listUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDTO)
                .toList();
    }

    public void resetPassword(Long userId, String newPassword) {
        passwordPolicy.validate(newPassword);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}