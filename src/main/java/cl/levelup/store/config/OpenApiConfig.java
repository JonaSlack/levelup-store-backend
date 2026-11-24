package cl.levelup.store.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "LevelUp Store API",
        version = "1.0.0",
        description = "API REST de la tienda LevelUp: autenticación con JWT, catálogo de juegos, pedidos y administración de usuarios.",
        contact = @Contact(name = "Equipo LevelUp Store")),
        servers = @Server(url = "/", description = "Mismo servidor que sirve la documentación"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
