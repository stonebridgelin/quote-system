package com.stonebridge.quotesystem.system.entity.dto;

import lombok.Data;

@Data
public class ResetPasswordDto {
    private String userId;
    private String newPassword;
}
