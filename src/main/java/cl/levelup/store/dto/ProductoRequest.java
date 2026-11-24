package cl.levelup.store.dto;

import jakarta.validation.constraints.*;

public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "El código solo admite minúsculas, números y guiones")
        @Size(max = 60)
        String codigo,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 120)
        String titulo,

        @NotBlank(message = "La plataforma es obligatoria")
        @Pattern(regexp = "^(ps5|pc|ambas)$", message = "La plataforma debe ser ps5, pc o ambas")
        String plataforma,

        @NotBlank(message = "El género es obligatorio")
        @Size(max = 80)
        String genero,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor a 0")
        Integer precio,

        @PositiveOrZero(message = "El precio anterior no puede ser negativo")
        Integer precioAnterior,

        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        @Size(max = 1000)
        String descripcion,

        @Size(max = 500)
        String imagen,

        @Size(max = 16)
        String emoji,

        Boolean destacado) {
}
