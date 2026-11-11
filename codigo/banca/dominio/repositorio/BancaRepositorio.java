package br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.repositorio;

import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.entidade.Banca;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.objetosvalor.BancaId;
import br.edu.com.fateczl.sistema.gerenciador.tg.grupotg.dominio.objetosvalor.GrupoTgId;

import java.util.List;
import java.util.Optional;

public interface BancaRepositorio {
    void salvar(Banca banca);
    Optional<Banca> buscarPorId(BancaId id);

    // Importante para garantir que um grupo não tenha duas bancas ativas ao mesmo tempo
    boolean existeBancaParaGrupo(GrupoTgId grupoId);

    Optional<Banca> buscarPorGrupoId(GrupoTgId id);

    List<Banca> buscarPorGruposId(List<GrupoTgId> grupoTgIds);
}

