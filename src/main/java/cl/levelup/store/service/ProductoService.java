package cl.levelup.store.service;

import cl.levelup.store.dto.ProductoRequest;
import cl.levelup.store.dto.ProductoResponse;
import cl.levelup.store.entity.Producto;
import cl.levelup.store.exception.ConflictoException;
import cl.levelup.store.exception.RecursoNoEncontradoException;
import cl.levelup.store.exception.ReglaNegocioException;
import cl.levelup.store.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ProductoService {

    private final ProductoRepository productos;

    public ProductoService(ProductoRepository productos) {
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String plataforma, Boolean oferta, String buscar) {
        String texto = buscar == null ? "" : buscar.trim().toLowerCase(Locale.ROOT);
        return productos.findByActivoTrueOrderByTituloAsc().stream()
                .filter(p -> plataforma == null || plataforma.isBlank()
                        || p.getPlataforma().equals(plataforma) || "ambas".equals(p.getPlataforma()))
                .filter(p -> oferta == null || !oferta || p.estaEnOferta())
                .filter(p -> texto.isEmpty()
                        || p.getTitulo().toLowerCase(Locale.ROOT).contains(texto)
                        || p.getGenero().toLowerCase(Locale.ROOT).contains(texto))
                .map(ProductoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorCodigo(String codigo) {
        return productos.findByCodigo(codigo)
                .filter(Producto::isActivo)
                .map(ProductoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + codigo));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        if (productos.existsByCodigo(req.codigo())) {
            throw new ConflictoException("Ya existe un producto con el código " + req.codigo());
        }
        Producto p = new Producto();
        aplicar(p, req);
        return ProductoResponse.desde(productos.save(p));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto p = buscarActivo(id);
        if (!p.getCodigo().equals(req.codigo()) && productos.existsByCodigo(req.codigo())) {
            throw new ConflictoException("Ya existe un producto con el código " + req.codigo());
        }
        aplicar(p, req);
        return ProductoResponse.desde(p);
    }

    @Transactional
    public void eliminar(Long id) {
        Producto p = buscarActivo(id);
        p.setActivo(false);
    }

    Producto buscarActivo(Long id) {
        return productos.findById(id)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + id));
    }

    private void aplicar(Producto p, ProductoRequest req) {
        if (req.precioAnterior() != null && req.precioAnterior() > 0 && req.precioAnterior() <= req.precio()) {
            throw new ReglaNegocioException("El precio anterior debe ser mayor al precio actual para marcar una oferta");
        }
        p.setCodigo(req.codigo());
        p.setTitulo(req.titulo().trim());
        p.setPlataforma(req.plataforma());
        p.setGenero(req.genero().trim());
        p.setPrecio(req.precio());
        p.setPrecioAnterior(req.precioAnterior() == null || req.precioAnterior() == 0 ? null : req.precioAnterior());
        p.setStock(req.stock());
        p.setDescripcion(req.descripcion());
        p.setImagen(req.imagen());
        p.setEmoji(req.emoji());
        p.setDestacado(Boolean.TRUE.equals(req.destacado()));
    }
}
