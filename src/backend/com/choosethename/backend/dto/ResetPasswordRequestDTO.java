package com.choosethename.backend.dto;

import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    private String newPassword;
}