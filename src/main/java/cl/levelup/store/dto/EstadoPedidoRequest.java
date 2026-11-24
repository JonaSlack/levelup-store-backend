package cl.levelup.store.dto;

import cl.levelup.store.entity.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record EstadoPedidoRequest(@NotNull(message = "El estado es obligatorio") EstadoPedido estado) {
}
