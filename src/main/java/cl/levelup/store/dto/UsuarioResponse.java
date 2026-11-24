package cl.levelup.store.dto;

import cl.levelup.store.entity.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(Long id, String username, String email, String nombre, String rol, boolean activo,
                              boolean bloqueado, LocalDateTime bloqueadoHasta) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.getNombre(), u.getRol().name(), u.isActivo(),
                u.estaBloqueado(), u.estaBloqueado() ? u.getBloqueadoHasta() : null);
    }
}
