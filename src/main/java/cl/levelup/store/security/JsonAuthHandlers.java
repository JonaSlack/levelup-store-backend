package cl.levelup.store.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JsonAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        escribir(res, 401, "Unauthorized", "Debes iniciar sesión o tu token expiró", req.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        escribir(res, 403, "Forbidden", "No tienes permisos para realizar esta acción", req.getRequestURI());
    }

    private void escribir(HttpServletResponse res, int status, String error, String mensaje, String ruta) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = String.format("{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"mensaje\":\"%s\",\"ruta\":\"%s\"}",
                Instant.now(), status, error, mensaje, ruta.replace("\"", ""));
        res.getWriter().write(json);
    }
}
