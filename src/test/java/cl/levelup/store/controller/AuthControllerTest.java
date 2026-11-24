package cl.levelup.store.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends ApiTestBase {

    @Test
    void loginCorrectoEntregaTokensYRol() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"goku\",\"password\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"));
    }

    @Test
    void loginAceptaCorreo() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin@levelup.cl\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"));
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"goku\",\"password\":\"0000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos. Te quedan 2 intentos"));
    }

    private ResultActions intentar(String usuario, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"%s\",\"password\":\"%s\"}".formatted(usuario, password)));
    }

    @Test
    void tresIntentosFallidosBloqueanLaCuenta() throws Exception {
        intentar("goku", "mala").andExpect(status().isUnauthorized());
        intentar("goku", "mala").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos. Te queda 1 intento"));
        intentar("goku", "mala").andExpect(status().isLocked())
                .andExpect(jsonPath("$.mensaje").value(containsString("Cuenta bloqueada")));

        intentar("goku", "1234").andExpect(status().isLocked());
    }

    @Test
    void loginCorrectoReiniciaLosIntentos() throws Exception {
        intentar("goku", "mala").andExpect(status().isUnauthorized());
        intentar("goku", "mala").andExpect(status().isUnauthorized());
        intentar("goku", "1234").andExpect(status().isOk());

        intentar("goku", "mala").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos. Te quedan 2 intentos"));
    }

    @Test
    void adminPuedeDesbloquearUnaCuenta() throws Exception {
        for (int i = 0; i < 3; i++) {
            intentar("goku", "mala");
        }
        String admin = tokenAdmin();
        String usuarios = mvc.perform(get("/api/usuarios").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.username == 'goku')].bloqueado").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = usuarios.replaceAll("(?s).*\\{\"id\":(\\d+),\"username\":\"goku\".*", "$1");

        mvc.perform(put("/api/usuarios/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"desbloquear\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueado").value(false));

        intentar("goku", "1234").andExpect(status().isOk());
    }

    @Test
    void registroCreaClienteYEvitaDuplicados() throws Exception {
        String body = "{\"nombre\":\"Vegeta\",\"username\":\"vegeta\",\"email\":\"vegeta@levelup.cl\",\"password\":\"saiyajin\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.username").value("vegeta"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void registroValidaCampos() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"username\":\"a\",\"email\":\"no-es-correo\",\"password\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.username").exists());
    }

    @Test
    void meRequiereToken() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/auth/me").header("Authorization", tokenCliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("goku"));
    }

    @Test
    void tokenAdulteradoEsRechazado() throws Exception {
        String token = tokenCliente();
        mvc.perform(get("/api/auth/me").header("Authorization", token.substring(0, token.length() - 3) + "abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotaElTokenYRevocaElAnterior() throws Exception {
        MvcResult inicio = login("goku", "1234");
        String refresh = refreshToken(inicio);
        String body = "{\"refreshToken\":\"" + refresh + "\"}";

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevocaElRefreshToken() throws Exception {
        String body = "{\"refreshToken\":\"" + refreshToken(login("goku", "1234")) + "\"}";
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}
