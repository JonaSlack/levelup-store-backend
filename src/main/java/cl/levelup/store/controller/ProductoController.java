package cl.levelup.store.controller;

import cl.levelup.store.dto.ProductoRequest;
import cl.levelup.store.dto.ProductoResponse;
import cl.levelup.store.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Catálogo de juegos. Lectura pública, escritura solo ADMIN.")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Permite filtrar por plataforma, ofertas y texto.")
    public List<ProductoResponse> listar(
            @Parameter(description = "ps5 o pc") @RequestParam(required = false) String plataforma,
            @Parameter(description = "true para ver solo ofertas") @RequestParam(required = false) Boolean oferta,
            @Parameter(description = "Texto a buscar en título o género") @RequestParam(required = false) String buscar) {
        return productoService.listar(plataforma, oferta, buscar);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por id")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @GetMapping("/codigo/{codigo}")
    @Operation(summary = "Obtener producto por código")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    public ProductoResponse obtenerPorCodigo(@PathVariable String codigo) {
        return productoService.obtenerPorCodigo(codigo);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear producto", description = "Solo ADMIN.")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @ApiResponse(responseCode = "403", description = "El usuario no es ADMIN")
    @ApiResponse(responseCode = "409", description = "Código duplicado")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        ProductoResponse creado = productoService.crear(req);
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(uri).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar producto", description = "Solo ADMIN.")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return productoService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar producto", description = "Solo ADMIN. El producto se desactiva para conservar el historial de pedidos.")
    @ApiResponse(responseCode = "204", description = "Producto eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
