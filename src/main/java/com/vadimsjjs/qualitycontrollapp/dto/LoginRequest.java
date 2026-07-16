package com.vadimsjjs.qualitycontrollapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {
    @NotNull(message = "Табельный номер обязателен")
    private Long personalNo;

    @NotNull(message = "Пароль обязателен")
    private String password;
}