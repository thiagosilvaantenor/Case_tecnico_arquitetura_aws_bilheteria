package br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.entidade;

import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.objetosvalor.BancaId;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.objetosvalor.MembroExterno;
import br.edu.com.fateczl.sistema.gerenciador.tg.banca.dominio.objetosvalor.StatusBanca;
import br.edu.com.fateczl.sistema.gerenciador.tg.compartilhado.dominio.excecoes.CodigoErro;
import br.edu.com.fateczl.sistema.gerenciador.tg.compartilhado.dominio.excecoes.RegraNegocioExcecao;
import br.edu.com.fateczl.sistema.gerenciador.tg.compartilhado.dominio.excecoes.ValidacaoExcecao;
import br.edu.com.fateczl.sistema.gerenciador.tg.grupotg.dominio.objetosvalor.GrupoTgId;
import br.edu.com.fateczl.sistema.gerenciador.tg.professor.dominio.objetosvalor.ProfessorId;

import java.time.LocalDateTime;
import java.util.*;

public class Banca {
    private final BancaId id;
    private final GrupoTgId grupoId;
    private LocalDateTime dataHora;
    private String local;
    private List<ProfessorId> avaliadoresInternos;
    private List<MembroExterno> avaliadoresExternos;
    private StatusBanca status;

    // Mapa guardando a nota de cada avaliador (A chave pode ser o ID do professor ou o email do membro externo)
    private Map<String, Double> notasMembros;
    private Double notaFinal;

    private Banca(
            BancaId id,
            GrupoTgId grupoId,
            LocalDateTime dataHora,
            String local,
            List<ProfessorId> avaliadoresInternos,
            List<MembroExterno> avaliadoresExternos,
            StatusBanca status,
            Map<String, Double> notasMembros,
            Double notaFinal
    ) {
        this.id = assegurarPresenca(id, "ID da banca");
        this.grupoId = assegurarPresenca(grupoId, "ID do grupo");
        this.dataHora = assegurarPresenca(dataHora, "data e hora");
        this.local = assegurarPresenca(local, "local");

        // Validação de regra de negócio: a banca precisa de membros
        validarComposicaoMinima(avaliadoresInternos, avaliadoresExternos);

        this.avaliadoresInternos = new ArrayList<>(avaliadoresInternos != null ? avaliadoresInternos : List.of());
        this.avaliadoresExternos = new ArrayList<>(avaliadoresExternos != null ? avaliadoresExternos : List.of());
        this.status = status;
        this.notasMembros = notasMembros != null ? new HashMap<>(notasMembros) : new HashMap<>();
        this.notaFinal = notaFinal;
    }

    // MÉTODOS FACTORY ---------------------------------------------------------

    public static Banca novo(
            BancaId id,
            GrupoTgId grupoId,
            LocalDateTime dataHora,
            String local,
            List<ProfessorId> avaliadoresInternos,
            List<MembroExterno> avaliadoresExternos
    ) {
        return new Banca(
                id, grupoId, dataHora, local, avaliadoresInternos,
                avaliadoresExternos, StatusBanca.MARCADA,
                new HashMap<>(), null);
    }

    public static Banca carregar(
            BancaId id,
            GrupoTgId grupoId,
            LocalDateTime dataHora,
            String local,
            List<ProfessorId> avaliadoresInternos,
            List<MembroExterno> avaliadoresExternos,
            StatusBanca status,
            Map<String, Double> notasMembros,
            Double notaFinal
    ) {
        return new Banca(id, grupoId, dataHora, local, avaliadoresInternos,
                avaliadoresExternos,status, notasMembros, notaFinal);
    }

    // MÉTODOS PARA GARANTIR PRESENÇA E VALIDAÇÃO ------------------------------

    private <T> T assegurarPresenca(T objeto, String campo) {
        if (objeto == null || (objeto instanceof String s && s.isBlank())) {
            throw new ValidacaoExcecao(CodigoErro.VD_001_CAMPO_OBRIGATORIO, campo);
        }
        return objeto;
    }

    private void validarComposicaoMinima(List<ProfessorId> internos, List<MembroExterno> externos) {
        boolean temInterno = internos != null && !internos.isEmpty();
        boolean temExterno = externos != null && !externos.isEmpty();

        //Se não tem interno e nem externo não tem nenhum avaliador
        if (!temInterno && !temExterno) {
            throw new RegraNegocioExcecao(
                    CodigoErro.GN_002_QUANTIDADE_INFERIOR,
                    "composição da banca", 1
            );
        }
    }

    // MÉTODOS DE ATUALIZAÇÃO --------------------------------------------------
    public void atribuirNotas(Map<String, Double> notas) {
        if (this.status == StatusBanca.CANCELADA) {
            throw new RegraNegocioExcecao(CodigoErro.RN_001_ESTADO_INVALIDO_PARA_ACAO, "banca", "MARCADA");
        }
        //Banca marcada que ainda não foi realizada
        if (this.dataHora.isAfter(LocalDateTime.now())) {
            throw new RegraNegocioExcecao(CodigoErro.RN_001_ESTADO_INVALIDO_PARA_ACAO, "data e hora de agendamento da banca", "posterior a data e hora atual");
        }

        this.notasMembros = new HashMap<>(notas);

        // Calcula a média das notas
        double soma = 0.0;
        for (Double nota : notas.values()) {
            soma += nota;
        }
        this.notaFinal = notas.isEmpty() ? 0.0 : (soma / notas.size());
        this.status = StatusBanca.AVALIADA;
    }

    public void cancelarAvaliacao(){
        //Só pode cancelar quando status == MARCADA
        if (this.status != StatusBanca.MARCADA) {
            throw new RegraNegocioExcecao(CodigoErro.RN_001_ESTADO_INVALIDO_PARA_ACAO, "banca", "MARCADA");
        }
        this.status = StatusBanca.CANCELADA;
    }

    public void alterarAgendamento(LocalDateTime novaDataHora, String novoLocal) {
        this.dataHora = assegurarPresenca(novaDataHora, "nova data e hora");
        this.local = assegurarPresenca(novoLocal, "novo local");
    }

    public void atualizarAvaliadores(List<ProfessorId> novosInternos, List<MembroExterno> novosExternos) {
        validarComposicaoMinima(novosInternos, novosExternos);
        this.avaliadoresInternos = new ArrayList<>(novosInternos != null ? novosInternos : List.of());
        this.avaliadoresExternos = new ArrayList<>(novosExternos != null ? novosExternos : List.of());
    }

    // MÉTODOS GETTERS DE DELEGAÇÃO --------------------------------------------

    public String idTexto() { return id.texto(); }
    public String grupoIdTexto() { return grupoId.texto(); }

    // MÉTODOS GETTERS ---------------------------------------------------------

    public BancaId id() { return id; }
    public GrupoTgId grupoId() { return grupoId; }
    public LocalDateTime dataHora() { return dataHora; }
    public String local() { return local; }

    public List<ProfessorId> avaliadoresInternos() {
        return Collections.unmodifiableList(avaliadoresInternos);
    }

    public List<MembroExterno> avaliadoresExternos() {
        return Collections.unmodifiableList(avaliadoresExternos);
    }
    public StatusBanca status() { return status; }
    public Double notaFinal() { return notaFinal; }
    public Map<String, Double> notasMembros() { return Collections.unmodifiableMap(notasMembros); }
}
