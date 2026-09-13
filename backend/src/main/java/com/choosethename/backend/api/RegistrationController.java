package com.choosethename.backend.api;

import com.choosethename.backend.dto.RegisterRequestDTO;
import com.choosethename.backend.dto.UserDTO;
import com.choosethename.backend.dto.UserMapper;
import com.choosethename.backend.model.User;
import com.choosethename.backend.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserMapper userMapper;

    public RegistrationController(RegistrationService registrationService, UserMapper userMapper) {
        this.registrationService = registrationService;
        this.userMapper = userMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequestDTO request) {
        User user = registrationService.registerParticipant(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDTO(user));
    }
}
