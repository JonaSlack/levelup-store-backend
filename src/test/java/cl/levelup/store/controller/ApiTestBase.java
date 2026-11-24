package cl.levelup.store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class ApiTestBase {

    private static final Pattern ACCESS = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
    private static final Pattern REFRESH = Pattern.compile("\"refreshToken\":\"([^\"]+)\"");

    @Autowired
    protected MockMvc mvc;

    protected MvcResult login(String usuario, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"%s\",\"password\":\"%s\"}".formatted(usuario, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    protected String tokenAdmin() throws Exception {
        return "Bearer " + extraer(ACCESS, login("admin", "admin123"));
    }

    protected String tokenCliente() throws Exception {
        return "Bearer " + extraer(ACCESS, login("goku", "1234"));
    }

    protected static String accessToken(MvcResult r) throws Exception {
        return extraer(ACCESS, r);
    }

    protected static String refreshToken(MvcResult r) throws Exception {
        return extraer(REFRESH, r);
    }

    private static String extraer(Pattern p, MvcResult r) throws Exception {
        Matcher m = p.matcher(r.getResponse().getContentAsString());
        if (!m.find()) {
            throw new IllegalStateException("La respuesta no contiene el token esperado");
        }
        return m.group(1);
    }
}
