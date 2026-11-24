package cl.levelup.store.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 10)
    private String plataforma;

    @Column(nullable = false, length = 80)
    private String genero;

    @Column(nullable = false)
    private Integer precio;

    private Integer precioAnterior;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(length = 1000)
    private String descripcion;

    @Column(length = 500)
    private String imagen;

    @Column(length = 16)
    private String emoji;

    @Column(nullable = false)
    private boolean destacado;

    @Column(nullable = false)
    private boolean activo = true;

    public boolean estaEnOferta() {
        return precioAnterior != null && precioAnterior > precio;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public Integer getPrecio() { return precio; }
    public void setPrecio(Integer precio) { this.precio = precio; }
    public Integer getPrecioAnterior() { return precioAnterior; }
    public void setPrecioAnterior(Integer precioAnterior) { this.precioAnterior = precioAnterior; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public boolean isDestacado() { return destacado; }
    public void setDestacado(boolean destacado) { this.destacado = destacado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
