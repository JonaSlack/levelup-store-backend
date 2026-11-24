package cl.levelup.store.controller;

import cl.levelup.store.dto.*;
import cl.levelup.store.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Login, registro y renovación de tokens JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Acepta nombre de usuario o correo. Devuelve access token y refresh token.")
    @ApiResponse(responseCode = "200", description = "Credenciales válidas")
    @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar cliente", description = "Crea una cuenta con rol CLIENTE e inicia sesión.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "409", description = "Usuario o correo ya registrado")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar tokens", description = "Entrega un nuevo access token y rota el refresh token (el anterior queda revocado).")
    @ApiResponse(responseCode = "200", description = "Tokens renovados")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado o revocado")
    public AuthResponse refrescar(@Valid @RequestBody RefreshRequest req) {
        return authService.refrescar(req.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token indicado.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Usuario actual", description = "Devuelve los datos del usuario dueño del token.")
    @ApiResponse(responseCode = "200", description = "Usuario autenticado")
    @ApiResponse(responseCode = "401", description = "Token ausente o expirado")
    public UsuarioResponse me(Authentication auth) {
        return authService.perfil(auth.getName());
    }
}
