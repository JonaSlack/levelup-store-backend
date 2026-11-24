package cl.levelup.store.service;

import cl.levelup.store.entity.MetodoEnvio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraEnvioTest {

    private final CalculadoraEnvio calculadora = new CalculadoraEnvio();

    @Test
    void cobraEnvioBajoElMinimo() {
        assertThat(calculadora.calcular(19990, MetodoEnvio.DOMICILIO)).isEqualTo(3990);
    }

    @Test
    void envioGratisDesdeElMinimo() {
        assertThat(calculadora.calcular(30000, MetodoEnvio.DOMICILIO)).isZero();
    }

    @Test
    void retiroEnTiendaNoCobra() {
        assertThat(calculadora.calcular(5000, MetodoEnvio.TIENDA)).isZero();
    }

    @Test
    void carritoVacioNoCobra() {
        assertThat(calculadora.calcular(0, MetodoEnvio.DOMICILIO)).isZero();
    }
}
