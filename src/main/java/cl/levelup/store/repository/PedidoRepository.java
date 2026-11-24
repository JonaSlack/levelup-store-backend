package cl.levelup.store.repository;

import cl.levelup.store.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    List<Pedido> findAllByOrderByFechaDesc();
}
