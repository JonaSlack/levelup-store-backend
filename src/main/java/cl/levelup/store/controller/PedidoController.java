package cl.levelup.store.controller;

import cl.levelup.store.dto.EstadoPedidoRequest;
import cl.levelup.store.dto.PedidoRequest;
import cl.levelup.store.dto.PedidoResponse;
import cl.levelup.store.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pedidos", description = "Compras de los clientes y gestión de estados")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN')")
    @Operation(summary = "Crear pedido", description = "Valida stock, lo descuenta y calcula envío y total en el servidor.")
    @ApiResponse(responseCode = "201", description = "Pedido creado")
    @ApiResponse(responseCode = "422", description = "Stock insuficiente o datos de envío incompletos")
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crear(auth.getName(), req));
    }

    @GetMapping("/mios")
    @Operation(summary = "Mis pedidos", description = "Pedidos del usuario autenticado.")
    public List<PedidoResponse> misPedidos(Authentication auth) {
        return pedidoService.misPedidos(auth.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los pedidos", description = "Solo ADMIN.")
    public List<PedidoResponse> todos() {
        return pedidoService.todos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pedido", description = "El cliente solo puede ver sus propios pedidos.")
    @ApiResponse(responseCode = "403", description = "El pedido pertenece a otro usuario")
    public PedidoResponse obtener(@PathVariable Long id, Authentication auth) {
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return pedidoService.obtener(id, auth.getName(), esAdmin);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado", description = "Solo ADMIN. Al cancelar se devuelve el stock.")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedidoRequest req) {
        return pedidoService.cambiarEstado(id, req.estado());
    }
}
