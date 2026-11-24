package cl.levelup.store.repository;

import cl.levelup.store.entity.RefreshToken;
import cl.levelup.store.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revocado = true where r.usuario = :usuario and r.revocado = false")
    int revocarTodos(Usuario usuario);
}
