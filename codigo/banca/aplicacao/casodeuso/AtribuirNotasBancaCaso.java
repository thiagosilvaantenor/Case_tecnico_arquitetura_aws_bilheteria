package br.edu.com.fateczl.sistema.gerenciador.tg.banca.aplicacao.casosdeuso;

import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.entidade.Banca;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.objetosvalor.BancaId;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.repositorio.BancaRepositorio;
import br.edu.com.fateczl.sistema.gerenciador.tg.compartilhado.aplicacao.eventos.BancaAvaliadaEvento;
import br.edu.com.fateczl.sistema.gerenciador.tg.compartilhado.aplicacao.portas.PublicadorEventos;
import br.edu.com.fateczl.sistema.gerenciador.tg.compartilhado.dominio.excecoes.CodigoErro;
import br.edu.com.fateczl.sistema.gerenciador.tg.compartilhado.dominio.excecoes.GenericaExcecao;
import br.edu.com.fateczl.sistema.gerenciador.tg.compartilhado.dominio.excecoes.RegraNegocioExcecao;
import br.edu.com.fateczl.sistema.gerenciador.tg.contausuario.dominio.objetosvalor.Email;
import br.edu.com.fateczl.sistema.gerenciador.tg.grupotg.dominio.entidade.GrupoTg;
import br.edu.com.fateczl.sistema.gerenciador.tg.grupotg.dominio.repositorio.GrupoTgRepositorio;
import br.edu.com.fateczl.sistema.gerenciador.tg.professor.dominio.entidade.Professor;
import br.edu.com.fateczl.sistema.gerenciador.tg.professor.dominio.repositorio.ProfessorRepositorio;

import java.util.Map;
import java.util.UUID;

public class AtribuirNotasBancaCaso {

    private final BancaRepositorio bancaRepositorio;
    private final GrupoTgRepositorio grupoTgRepositorio;
    private final ProfessorRepositorio professorRepositorio;
    private final PublicadorEventos publicador;

    public AtribuirNotasBancaCaso(
            BancaRepositorio bancaRepositorio,
            GrupoTgRepositorio grupoTgRepositorio,
            ProfessorRepositorio professorRepositorio,
            PublicadorEventos publicador
    ) {
        this.bancaRepositorio = bancaRepositorio;
        this.grupoTgRepositorio = grupoTgRepositorio;
        this.professorRepositorio = professorRepositorio;
        this.publicador = publicador;
    }

    public record Comando(
            String emailOrientadorLogado,
            String idBanca,
            Map<String, Double> notasDosMembros // Chave: ID do Prof ou Email Externo | Valor: Nota (0 a 10)
    ) {}

    public void executar(Comando comando) {
        // Validações de segurança do Orientador
        Professor orientadorLogado = professorRepositorio.buscarPorEmail(new Email(comando.emailOrientadorLogado()))
                .orElseThrow(() -> new GenericaExcecao(CodigoErro.GN_001_REGISTRO_NAO_ENCONTRADO, "orientador"));

        Banca banca = bancaRepositorio.buscarPorId(new BancaId(UUID.fromString(comando.idBanca())))
                .orElseThrow(() -> new GenericaExcecao(CodigoErro.GN_001_REGISTRO_NAO_ENCONTRADO, "banca"));

        GrupoTg grupo = grupoTgRepositorio.buscarPorIdGrupo(banca.grupoId())
                .orElseThrow(() -> new GenericaExcecao(CodigoErro.GN_001_REGISTRO_NAO_ENCONTRADO, "grupo"));

        if (!grupo.orientadorId().equals(orientadorLogado.id())) {
            throw new
                    RegraNegocioExcecao(CodigoErro.RN_001_ESTADO_INVALIDO_PARA_ACAO,
                    "notas", "atribuídas por alguém que não é o orientador oficial");
        }

        // Aciona o Domínio para fazer a regra matemática e de status
        banca.atribuirNotas(comando.notasDosMembros());

        // Salva a Banca atualizada
        bancaRepositorio.salvar(banca);

        // Dispara o evento para os ouvintes (Isso acorda a nossa classe de certificados)
        publicador.publicar(new BancaAvaliadaEvento(banca.idTexto()));
    }
}
