package cl.levelup.store.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Ingresa tu usuario o correo") String usuario,
        @NotBlank(message = "Ingresa tu contraseña") String password) {
}
