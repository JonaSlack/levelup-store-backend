package cl.levelup.store.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,
        @NotNull @Min(value = 1, message = "La cantidad mínima es 1") @Max(value = 10, message = "La cantidad máxima es 10")
        Integer cantidad) {
}
