package cl.levelup.store.service;

import cl.levelup.store.dto.ItemPedidoRequest;
import cl.levelup.store.dto.PedidoRequest;
import cl.levelup.store.dto.PedidoResponse;
import cl.levelup.store.entity.*;
import cl.levelup.store.exception.RecursoNoEncontradoException;
import cl.levelup.store.exception.ReglaNegocioException;
import cl.levelup.store.repository.PedidoRepository;
import cl.levelup.store.repository.ProductoRepository;
import cl.levelup.store.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final CalculadoraEnvio calculadoraEnvio;

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos, UsuarioRepository usuarios,
                         CalculadoraEnvio calculadoraEnvio) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.usuarios = usuarios;
        this.calculadoraEnvio = calculadoraEnvio;
    }

    @Transactional
    public PedidoResponse crear(String username, PedidoRequest req) {
        Usuario usuario = usuarios.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (req.metodoEnvio() == MetodoEnvio.DOMICILIO
                && (vacio(req.direccion()) || vacio(req.comuna()))) {
            throw new ReglaNegocioException("Para envío a domicilio debes indicar dirección y comuna");
        }

        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemPedidoRequest item : req.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setMetodoEnvio(req.metodoEnvio());
        pedido.setDireccion(req.metodoEnvio() == MetodoEnvio.DOMICILIO ? req.direccion().trim() : null);
        pedido.setComuna(req.metodoEnvio() == MetodoEnvio.DOMICILIO ? req.comuna().trim() : null);
        pedido.setTelefono(req.telefono());
        pedido.setMetodoPago(req.metodoPago());

        int subtotal = 0;
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto producto = productos.findById(e.getKey())
                    .filter(Producto::isActivo)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + e.getKey()));
            int cantidad = e.getValue();
            if (producto.getStock() < cantidad) {
                throw new ReglaNegocioException("Stock insuficiente para " + producto.getTitulo()
                        + " (disponible: " + producto.getStock() + ")");
            }
            producto.setStock(producto.getStock() - cantidad);

            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio() * cantidad);
            pedido.agregarDetalle(detalle);
            subtotal += detalle.getSubtotal();
        }

        int envio = calculadoraEnvio.calcular(subtotal, req.metodoEnvio());
        pedido.setSubtotal(subtotal);
        pedido.setEnvio(envio);
        pedido.setTotal(subtotal + envio);
        pedido.setEstado(EstadoPedido.PAGADO);
        return PedidoResponse.desde(pedidos.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> misPedidos(String username) {
        Usuario usuario = usuarios.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return pedidos.findByUsuarioIdOrderByFechaDesc(usuario.getId()).stream().map(PedidoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> todos() {
        return pedidos.findAllByOrderByFechaDesc().stream().map(PedidoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id, String username, boolean esAdmin) {
        Pedido p = buscar(id);
        if (!esAdmin && !p.getUsuario().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("El pedido no te pertenece");
        }
        return PedidoResponse.desde(p);
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevo) {
        Pedido p = buscar(id);
        if (p.getEstado() == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("Un pedido cancelado no puede cambiar de estado");
        }
        if (p.getEstado() == EstadoPedido.ENTREGADO && nuevo == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("Un pedido entregado no se puede cancelar");
        }
        if (nuevo == EstadoPedido.CANCELADO) {
            p.getDetalles().forEach(d -> d.getProducto().setStock(d.getProducto().getStock() + d.getCantidad()));
        }
        p.setEstado(nuevo);
        return PedidoResponse.desde(p);
    }

    private Pedido buscar(Long id) {
        return pedidos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + id));
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
