package cl.levelup.store.config;

import cl.levelup.store.entity.Producto;
import cl.levelup.store.entity.Rol;
import cl.levelup.store.entity.Usuario;
import cl.levelup.store.repository.ProductoRepository;
import cl.levelup.store.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarios;
    private final ProductoRepository productos;
    private final PasswordEncoder encoder;
    private final ObjectMapper mapper;

    public DataInitializer(UsuarioRepository usuarios, ProductoRepository productos,
                           PasswordEncoder encoder, ObjectMapper mapper) {
        this.usuarios = usuarios;
        this.productos = productos;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        crearUsuario("admin", "admin@levelup.cl", "Administrador LevelUp", "admin123", Rol.ADMIN);
        crearUsuario("goku", "goku@levelup.cl", "Goku", "1234", Rol.CLIENTE);
        if (productos.count() == 0) {
            cargarCatalogo();
        }
    }

    private void crearUsuario(String username, String email, String nombre, String clave, Rol rol) {
        if (usuarios.existsByUsernameIgnoreCase(username)) {
            return;
        }
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setEmail(email);
        u.setNombre(nombre);
        u.setPassword(encoder.encode(clave));
        u.setRol(rol);
        usuarios.save(u);
        log.info("Usuario inicial creado: {} ({})", username, rol);
    }

    private void cargarCatalogo() throws IOException {
        try (InputStream in = new ClassPathResource("catalogo-inicial.json").getInputStream()) {
            List<Map<String, Object>> juegos = mapper.readValue(in, new TypeReference<>() {});
            for (Map<String, Object> j : juegos) {
                Producto p = new Producto();
                p.setCodigo((String) j.get("codigo"));
                p.setTitulo((String) j.get("titulo"));
                p.setPlataforma((String) j.get("plataforma"));
                p.setGenero((String) j.get("genero"));
                p.setPrecio(((Number) j.get("precio")).intValue());
                Object anterior = j.get("precioAnterior");
                p.setPrecioAnterior(anterior == null ? null : ((Number) anterior).intValue());
                p.setStock(((Number) j.get("stock")).intValue());
                p.setDescripcion((String) j.get("descripcion"));
                p.setImagen((String) j.get("imagen"));
                p.setEmoji((String) j.get("emoji"));
                p.setDestacado(Boolean.TRUE.equals(j.get("destacado")));
                productos.save(p);
            }
            log.info("Catálogo inicial cargado: {} productos", juegos.size());
        }
    }
}
