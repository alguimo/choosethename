package com.choosethename.backend.service;

import com.choosethename.backend.exception.UserNotFoundException;
import com.choosethename.backend.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public CurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long requireUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"))
                .getId();
    }
}