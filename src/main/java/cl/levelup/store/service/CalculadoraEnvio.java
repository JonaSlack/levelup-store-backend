package cl.levelup.store.service;

import cl.levelup.store.entity.MetodoEnvio;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraEnvio {

    public static final int COSTO_ENVIO = 3990;
    public static final int MINIMO_ENVIO_GRATIS = 30000;

    public int calcular(int subtotal, MetodoEnvio metodo) {
        if (subtotal <= 0 || metodo == MetodoEnvio.TIENDA || subtotal >= MINIMO_ENVIO_GRATIS) {
            return 0;
        }
        return COSTO_ENVIO;
    }
}
