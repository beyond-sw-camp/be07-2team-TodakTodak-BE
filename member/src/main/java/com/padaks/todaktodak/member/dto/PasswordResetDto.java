package com.padaks.todaktodak.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetDto {
    private String newPassword;
    private String confirmPassword;
    private String token;
}
