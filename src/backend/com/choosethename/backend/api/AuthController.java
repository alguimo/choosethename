package com.choosethename.backend.api;

import com.choosethename.backend.dto.AuthResponseDTO;
import com.choosethename.backend.dto.LoginRequestDTO;
import com.choosethename.backend.dto.UserDTO;
import com.choosethename.backend.dto.UserMapper;
import com.choosethename.backend.exception.UserNotFoundException;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.security.LoginRateLimiter;
import com.choosethename.backend.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthenticationService authenticationService, UserRepository userRepository, UserMapper userMapper, LoginRateLimiter loginRateLimiter) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (loginRateLimiter.isBlocked(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-Rate-Limit-Exceeded", "true")
                    .build();
        }
        try {
            AuthResponseDTO response = authenticationService.authenticate(request);
            loginRateLimiter.onSuccess(clientIp);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            loginRateLimiter.onFailure(clientIp);
            throw ex;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return ResponseEntity.ok(userMapper.toDTO(user));
    }
}
