package cl.levelup.store.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductoControllerTest extends ApiTestBase {

    private static final String NUEVO = """
            {"codigo":"hollow-knight","titulo":"Hollow Knight","plataforma":"pc","genero":"Metroidvania",
             "precio":9990,"stock":30,"descripcion":"Explora Hallownest","emoji":"🦋","destacado":false}
            """;

    @Test
    void catalogoEsPublico() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)));
    }

    @Test
    void filtraOfertas() throws Exception {
        mvc.perform(get("/api/productos").param("oferta", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].oferta", everyItem(is(true))));
    }

    @Test
    void obtienePorCodigoY404SiNoExiste() throws Exception {
        mvc.perform(get("/api/productos/codigo/minecraft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Minecraft"));
        mvc.perform(get("/api/productos/9999")).andExpect(status().isNotFound());
    }

    @Test
    void crearSinTokenDevuelve401() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        mvc.perform(post("/api/productos").header("Authorization", tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminHaceCrudCompleto() throws Exception {
        String admin = tokenAdmin();

        String creado = mvc.perform(post("/api/productos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("hollow-knight"))
                .andReturn().getResponse().getContentAsString();
        String id = creado.replaceAll("^\\{\"id\":(\\d+).*$", "$1");

        mvc.perform(put("/api/productos/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVO.replace("\"precio\":9990", "\"precio\":7990,\"precioAnterior\":9990")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(7990))
                .andExpect(jsonPath("$.oferta").value(true));

        mvc.perform(delete("/api/productos/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/productos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void codigoDuplicadoDevuelve409() throws Exception {
        mvc.perform(post("/api/productos").header("Authorization", tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVO.replace("hollow-knight", "minecraft")))
                .andExpect(status().isConflict());
    }

    @Test
    void precioInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/productos").header("Authorization", tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVO.replace("\"precio\":9990", "\"precio\":-5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.precio").exists());
    }
}
