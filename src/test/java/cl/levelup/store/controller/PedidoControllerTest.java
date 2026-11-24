package cl.levelup.store.controller;

import cl.levelup.store.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PedidoControllerTest extends ApiTestBase {

    @Autowired
    private ProductoRepository productos;

    private Long idDe(String codigo) {
        return productos.findByCodigo(codigo).orElseThrow().getId();
    }

    private String pedido(String codigo, int cantidad, String envio) {
        return """
                {"items":[{"productoId":%d,"cantidad":%d}],"metodoEnvio":"%s",
                 "direccion":"Av. Siempre Viva 742","comuna":"Santiago","telefono":"+56911111111","metodoPago":"debito"}
                """.formatted(idDe(codigo), cantidad, envio);
    }

    @Test
    void crearPedidoDescuentaStockYCalculaEnvio() throws Exception {
        int stockAntes = productos.findByCodigo("minecraft").orElseThrow().getStock();

        mvc.perform(post("/api/pedidos").header("Authorization", tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON).content(pedido("minecraft", 1, "DOMICILIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(19990))
                .andExpect(jsonPath("$.envio").value(3990))
                .andExpect(jsonPath("$.total").value(23980))
                .andExpect(jsonPath("$.estado").value("PAGADO"));

        assertThat(productos.findByCodigo("minecraft").orElseThrow().getStock()).isEqualTo(stockAntes - 1);
    }

    @Test
    void envioGratisSobreElMinimoYRetiroEnTienda() throws Exception {
        String cliente = tokenCliente();
        mvc.perform(post("/api/pedidos").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(pedido("gow-ragnarok", 1, "DOMICILIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.envio").value(0));

        mvc.perform(post("/api/pedidos").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(pedido("minecraft", 1, "TIENDA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.envio").value(0))
                .andExpect(jsonPath("$.direccion").doesNotExist());
    }

    @Test
    void stockInsuficienteDevuelve422() throws Exception {
        var p = productos.findByCodigo("minecraft").orElseThrow();
        p.setStock(1);
        productos.save(p);

        mvc.perform(post("/api/pedidos").header("Authorization", tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON).content(pedido("minecraft", 2, "DOMICILIO")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void pedidoRequiereSesion() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido("minecraft", 1, "TIENDA")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteVeSoloSusPedidosYNoElListadoGeneral() throws Exception {
        String cliente = tokenCliente();
        mvc.perform(post("/api/pedidos").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(pedido("minecraft", 1, "TIENDA")))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/pedidos/mios").header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cliente").value("goku"));

        mvc.perform(get("/api/pedidos").header("Authorization", cliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCambiaEstadoYCancelarDevuelveStock() throws Exception {
        int stockAntes = productos.findByCodigo("witcher3").orElseThrow().getStock();
        String creado = mvc.perform(post("/api/pedidos").header("Authorization", tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON).content(pedido("witcher3", 2, "TIENDA")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = creado.replaceAll("^\\{\"id\":(\\d+).*$", "$1");
        String admin = tokenAdmin();

        mvc.perform(get("/api/pedidos").header("Authorization", admin))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/pedidos/" + id + "/estado").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        assertThat(productos.findByCodigo("witcher3").orElseThrow().getStock()).isEqualTo(stockAntes);
    }

    @Test
    void clienteNoPuedeCambiarEstado() throws Exception {
        mvc.perform(patch("/api/pedidos/1/estado").header("Authorization", tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENVIADO\"}"))
                .andExpect(status().isForbidden());
    }
}
