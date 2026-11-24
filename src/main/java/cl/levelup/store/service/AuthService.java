package cl.levelup.store.service;

import cl.levelup.store.dto.AuthResponse;
import cl.levelup.store.dto.LoginRequest;
import cl.levelup.store.dto.RegistroRequest;
import cl.levelup.store.dto.UsuarioResponse;
import cl.levelup.store.entity.RefreshToken;
import cl.levelup.store.entity.Rol;
import cl.levelup.store.entity.Usuario;
import cl.levelup.store.exception.ConflictoException;
import cl.levelup.store.exception.CuentaBloqueadaException;
import cl.levelup.store.exception.RecursoNoEncontradoException;
import cl.levelup.store.exception.TokenInvalidoException;
import cl.levelup.store.repository.RefreshTokenRepository;
import cl.levelup.store.repository.UsuarioRepository;
import cl.levelup.store.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final AuthenticationManager authManager;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final Duration duracionRefresh;
    private final int maxIntentos;
    private final Duration duracionBloqueo;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
                       AuthenticationManager authManager, PasswordEncoder encoder, JwtService jwtService,
                       @Value("${app.jwt.refresh-days:7}") long diasRefresh,
                       @Value("${app.login.max-intentos:3}") int maxIntentos,
                       @Value("${app.login.bloqueo-minutos:5}") long minutosBloqueo) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.authManager = authManager;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.duracionRefresh = Duration.ofDays(diasRefresh);
        this.maxIntentos = maxIntentos;
        this.duracionBloqueo = Duration.ofMinutes(minutosBloqueo);
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class, CuentaBloqueadaException.class})
    public AuthResponse login(LoginRequest req) {
        String login = req.usuario().trim();
        Usuario cuenta = usuarios.findByUsernameIgnoreCase(login)
                .or(() -> usuarios.findByEmailIgnoreCase(login))
                .orElse(null);

        if (cuenta != null && cuenta.estaBloqueado()) {
            throw new CuentaBloqueadaException(mensajeBloqueo(cuenta));
        }

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(login, req.password()));
        } catch (DisabledException e) {
            throw e;
        } catch (AuthenticationException e) {
            registrarFallo(cuenta);
        }

        cuenta.limpiarBloqueo();
        return emitirTokens(cuenta);
    }

    private void registrarFallo(Usuario cuenta) {
        if (cuenta == null) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }
        int intentos = cuenta.getIntentosFallidos() + 1;
        if (intentos >= maxIntentos) {
            cuenta.setIntentosFallidos(0);
            cuenta.setBloqueadoHasta(LocalDateTime.now().plus(duracionBloqueo));
            throw new CuentaBloqueadaException(mensajeBloqueo(cuenta));
        }
        cuenta.setIntentosFallidos(intentos);
        int restantes = maxIntentos - intentos;
        throw new BadCredentialsException("Usuario o contraseña incorrectos. Te queda" + (restantes == 1 ? "" : "n")
                + " " + restantes + " intento" + (restantes == 1 ? "" : "s"));
    }

    private String mensajeBloqueo(Usuario cuenta) {
        long segundos = Duration.between(LocalDateTime.now(), cuenta.getBloqueadoHasta()).getSeconds();
        long minutos = Math.max(1, (segundos + 59) / 60);
        return "Cuenta bloqueada por demasiados intentos fallidos. Intenta de nuevo en " + minutos
                + " minuto" + (minutos == 1 ? "" : "s");
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        if (usuarios.existsByUsernameIgnoreCase(req.username())) {
            throw new ConflictoException("El nombre de usuario ya está en uso");
        }
        if (usuarios.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictoException("El correo ya está registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setUsername(req.username().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setPassword(encoder.encode(req.password()));
        u.setRol(Rol.CLIENTE);
        usuarios.save(u);
        return emitirTokens(u);
    }

    @Transactional
    public AuthResponse refrescar(String token) {
        RefreshToken actual = refreshTokens.findByToken(token)
                .orElseThrow(() -> new TokenInvalidoException("Refresh token inválido"));
        if (!actual.esValido()) {
            throw new TokenInvalidoException("Refresh token expirado o revocado");
        }
        Usuario u = actual.getUsuario();
        if (!u.isActivo()) {
            throw new TokenInvalidoException("La cuenta está deshabilitada");
        }
        actual.setRevocado(true);
        return emitirTokens(u);
    }

    @Transactional
    public void logout(String token) {
        refreshTokens.findByToken(token).ifPresent(r -> r.setRevocado(true));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String username) {
        return UsuarioResponse.desde(buscarPorLogin(username));
    }

    private Usuario buscarPorLogin(String login) {
        return usuarios.findByUsernameIgnoreCase(login)
                .or(() -> usuarios.findByEmailIgnoreCase(login))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private AuthResponse emitirTokens(Usuario u) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        RefreshToken r = new RefreshToken();
        r.setToken(HexFormat.of().formatHex(bytes));
        r.setUsuario(u);
        r.setExpiraEn(Instant.now().plus(duracionRefresh));
        refreshTokens.save(r);
        return new AuthResponse(jwtService.generarToken(u), r.getToken(), "Bearer",
                jwtService.getSegundosAcceso(), UsuarioResponse.desde(u));
    }
}
