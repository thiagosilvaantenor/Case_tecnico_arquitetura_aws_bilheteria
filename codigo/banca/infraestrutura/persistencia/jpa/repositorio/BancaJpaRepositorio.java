package br.edu.com.fateczl.sistema.gerenciador.tg.banca.infraestrutura.persistencia.jpa.repositorio;

import br.edu.com.fateczl.sistema.gerenciador.tg.banca.infraestrutura.persistencia.jpa.modelo.BancaModelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BancaJpaRepositorio extends JpaRepository<BancaModelo, String> {
    boolean existsByGrupoId(String grupoId);

    Optional<BancaModelo> findByGrupoId(String id);

    List<BancaModelo> findAllByGrupoIdIn(List<String> grupoIds);
}
