package cl.levelup.store.service;

import cl.levelup.store.entity.Rol;
import cl.levelup.store.entity.Usuario;
import cl.levelup.store.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-levelup-store-con-32-bytes-o-mas";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setUsername("goku");
        u.setNombre("Goku");
        u.setRol(Rol.CLIENTE);
        return u;
    }

    @Test
    void generaYValidaToken() {
        JwtService jwt = new JwtService(SECRETO, 15, "levelup-store");
        var claims = jwt.validar(jwt.generarToken(usuario())).orElseThrow();
        assertThat(claims.getSubject()).isEqualTo("goku");
        assertThat(claims.get("rol", String.class)).isEqualTo("CLIENTE");
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtService otro = new JwtService("otra-clave-distinta-tambien-de-32-bytes-o-mas", 15, "levelup-store");
        JwtService jwt = new JwtService(SECRETO, 15, "levelup-store");
        assertThat(jwt.validar(otro.generarToken(usuario()))).isEmpty();
    }

    @Test
    void rechazaTokenExpirado() {
        JwtService jwt = new JwtService(SECRETO, 0, "levelup-store");
        assertThat(jwt.validar(jwt.generarToken(usuario()))).isEmpty();
    }

    @Test
    void exigeClaveSegura() {
        assertThatThrownBy(() -> new JwtService("corta", 15, "levelup-store"))
                .isInstanceOf(IllegalStateException.class);
    }
}
