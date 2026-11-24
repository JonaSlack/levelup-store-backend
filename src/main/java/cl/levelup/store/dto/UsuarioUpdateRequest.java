package cl.levelup.store.dto;

import cl.levelup.store.entity.Rol;

public record UsuarioUpdateRequest(Rol rol, Boolean activo, Boolean desbloquear) {
}
