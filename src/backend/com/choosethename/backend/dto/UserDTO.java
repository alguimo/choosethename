package com.choosethename.backend.dto;

import com.choosethename.backend.model.Role;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private Role role;
}
