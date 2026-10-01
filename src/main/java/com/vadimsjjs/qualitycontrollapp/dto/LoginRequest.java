package com.vadimsjjs.qualitycontrollapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequest {
    @NotNull(message = "Табельный номер обязателен")
    private Long personalNo;

    @NotNull(message = "Пароль обязателен")
    private String password;
}