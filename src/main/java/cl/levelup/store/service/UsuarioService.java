package cl.levelup.store.service;

import cl.levelup.store.dto.UsuarioResponse;
import cl.levelup.store.dto.UsuarioUpdateRequest;
import cl.levelup.store.entity.Usuario;
import cl.levelup.store.exception.RecursoNoEncontradoException;
import cl.levelup.store.exception.ReglaNegocioException;
import cl.levelup.store.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest req, String solicitante) {
        Usuario u = buscar(id);
        boolean esElMismo = u.getUsername().equalsIgnoreCase(solicitante);
        if (esElMismo && (req.activo() != null && !req.activo() || req.rol() != null && req.rol() != u.getRol())) {
            throw new ReglaNegocioException("No puedes cambiar tu propio rol ni desactivar tu cuenta");
        }
        if (req.rol() != null) {
            u.setRol(req.rol());
        }
        if (req.activo() != null) {
            u.setActivo(req.activo());
        }
        if (Boolean.TRUE.equals(req.desbloquear())) {
            u.limpiarBloqueo();
        }
        return UsuarioResponse.desde(u);
    }

    @Transactional
    public void eliminar(Long id, String solicitante) {
        Usuario u = buscar(id);
        if (u.getUsername().equalsIgnoreCase(solicitante)) {
            throw new ReglaNegocioException("No puedes eliminar tu propia cuenta");
        }
        u.setActivo(false);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
    }
}
