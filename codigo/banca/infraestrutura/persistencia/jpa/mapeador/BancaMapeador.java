package br.edu.com.fateczl.sistema.gerenciador.tg.banca.infraestrutura.persistencia.jpa.mapeador;

import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.entidade.Banca;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.objetosvalor.BancaId;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.infraestrutura.persistencia.jpa.modelo.BancaModelo;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.infraestrutura.persistencia.jpa.modelo.MembroExternoModelo;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.objetosvalor.MembroExterno;
import br.edu.com.fateczl.sistema.gerenciador.tg.contausuario.dominio.objetosvalor.Email;
import br.edu.com.fateczl.sistema.gerenciador.tg.grupotg.dominio.objetosvalor.GrupoTgId;
import br.edu.com.fateczl.sistema.gerenciador.tg.professor.dominio.objetosvalor.ProfessorId;

import java.util.HashMap;
import java.util.UUID;

public class BancaMapeador {
    private BancaMapeador(){}


    public static BancaModelo paraModelo(Banca banca) {
        BancaModelo modelo = new BancaModelo();
        modelo.setId(banca.id().texto());
        modelo.setGrupoId(banca.grupoId().texto());
        modelo.setDataHora(banca.dataHora());
        modelo.setLocal(banca.local());

        modelo.setAvaliadoresInternosIds(
                banca.avaliadoresInternos().stream()
                        .map(ProfessorId::texto)
                        .toList()
        );

        modelo.setAvaliadoresExternos(
                banca.avaliadoresExternos().stream()
                        .map(ext -> new MembroExternoModelo(ext.nome(), ext.email(), ext.telefone()))
                        .toList()
        );
        modelo.setStatusBanca(banca.status());
        // TRATATIVA CRÍTICA: Desempacota o unmodifiableMap do Domínio em um HashMap mutável para o Hibernate
        modelo.setNotasMembros(
                banca.notasMembros() != null ? new HashMap<>(banca.notasMembros()) : new HashMap<>()
        );
        modelo.setNotaFinal(banca.notaFinal());

        return modelo;
    }

    public static Banca paraDominio(BancaModelo modelo) {
        return Banca.carregar(
                new BancaId(UUID.fromString(modelo.getId())),
                new GrupoTgId(UUID.fromString(modelo.getGrupoId())),
                modelo.getDataHora(),
                modelo.getLocal(),
                modelo.getAvaliadoresInternosIds().stream()
                        .map(idStr -> new ProfessorId(UUID.fromString(idStr)))
                        .toList(),
                modelo.getAvaliadoresExternos().stream()
                        .map(membro -> new MembroExterno(
                                membro.getNome(),
                                membro.getEmail(),
                                membro.getTelefone()))
                        .toList(),
                modelo.getStatusBanca(),
                // Se for null, passa um mapa vazio. O construtor de Banca também fará a própria cópia de segurança.
                modelo.getNotasMembros() != null ? modelo.getNotasMembros() : new HashMap<>(),
                modelo.getNotaFinal()
        );
    }
}
