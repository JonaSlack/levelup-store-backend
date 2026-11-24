package cl.levelup.store.dto;

import cl.levelup.store.entity.Producto;

public record ProductoResponse(
        Long id,
        String codigo,
        String titulo,
        String plataforma,
        String genero,
        Integer precio,
        Integer precioAnterior,
        boolean oferta,
        Integer stock,
        String descripcion,
        String imagen,
        String emoji,
        boolean destacado) {

    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getTitulo(), p.getPlataforma(), p.getGenero(),
                p.getPrecio(), p.getPrecioAnterior(), p.estaEnOferta(), p.getStock(), p.getDescripcion(),
                p.getImagen(), p.getEmoji(), p.isDestacado());
    }
}
