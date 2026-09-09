package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Long personalNo;
    private String fio;
    private List<String> roles;
    private String message;
    private boolean locked;
    private long remainingTimeMinutes;

    public static AuthResponse success(Long personalNo, String fio, List<String> roles) {
        return new AuthResponse(personalNo, fio, roles, "Вход выполнен успешно", false, 0);
    }

    public static AuthResponse logout() {
        return new AuthResponse(null, null, null, "Выход выполнен успешно", false, 0);
    }

    public static AuthResponse unauthorized() {
        return new AuthResponse(null, null, null, "Пользователь не аутентифицирован", false, 0);
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(null, null, null, message, false, 0);
    }

    public static AuthResponse locked(String message, long remainingTimeMinutes) {
        return new AuthResponse(null, null, null, message, true, remainingTimeMinutes);
    }
}
