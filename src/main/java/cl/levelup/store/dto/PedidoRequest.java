package cl.levelup.store.dto;

import cl.levelup.store.entity.MetodoEnvio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoRequest(
        @NotEmpty(message = "El pedido debe tener al menos un producto") List<@Valid ItemPedidoRequest> items,
        @NotNull(message = "Selecciona un método de envío") MetodoEnvio metodoEnvio,
        @Size(max = 200) String direccion,
        @Size(max = 80) String comuna,
        @Size(max = 20) String telefono,
        @Size(max = 30) String metodoPago) {
}
