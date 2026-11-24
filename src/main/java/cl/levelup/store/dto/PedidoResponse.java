package cl.levelup.store.dto;

import cl.levelup.store.entity.DetallePedido;
import cl.levelup.store.entity.Pedido;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        LocalDateTime fecha,
        String estado,
        String metodoEnvio,
        String direccion,
        String comuna,
        String telefono,
        String metodoPago,
        Integer subtotal,
        Integer envio,
        Integer total,
        String cliente,
        List<Item> items) {

    public record Item(Long productoId, String codigo, String titulo, Integer cantidad, Integer precioUnitario, Integer subtotal) {

        static Item desde(DetallePedido d) {
            return new Item(d.getProducto().getId(), d.getProducto().getCodigo(), d.getProducto().getTitulo(),
                    d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
        }
    }

    public static PedidoResponse desde(Pedido p) {
        return new PedidoResponse(p.getId(), p.getFecha(), p.getEstado().name(), p.getMetodoEnvio().name(),
                p.getDireccion(), p.getComuna(), p.getTelefono(), p.getMetodoPago(),
                p.getSubtotal(), p.getEnvio(), p.getTotal(), p.getUsuario().getUsername(),
                p.getDetalles().stream().map(Item::desde).toList());
    }
}
