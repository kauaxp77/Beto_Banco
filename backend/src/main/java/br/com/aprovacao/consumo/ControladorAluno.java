package br.com.aprovacao.consumo;

import br.com.aprovacao.auth.Usuario;
import br.com.aprovacao.auth.UsuarioRepository;
import br.com.aprovacao.catalogo.Curso;
import br.com.aprovacao.catalogo.CursoRepository;
import br.com.aprovacao.comercial.DtosComercial.MatriculaResponse;
import br.com.aprovacao.common.ProblemaNegocio;
import br.com.aprovacao.config.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Secao 19 -- rotas do aluno: matriculas, player e progresso. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Aluno", description = "Area do aluno: matriculas, player e progresso (secoes 09, 10 e 19)")
public class ControladorAluno {

    private final MatriculaRepository matriculas;
    private final CursoRepository cursos;
    private final UsuarioRepository usuarios;
    private final ProgressoAulaRepository progressos;
    private final ServicoPlayer player;

    public ControladorAluno(MatriculaRepository matriculas,
                            CursoRepository cursos,
                            UsuarioRepository usuarios,
                            ProgressoAulaRepository progressos,
                            ServicoPlayer player) {
        this.matriculas = matriculas;
        this.cursos = cursos;
        this.usuarios = usuarios;
        this.progressos = progressos;
        this.player = player;
    }

    @GetMapping("/me/matriculas")
    @Operation(summary = "Cursos liberados e validade")
    public List<MatriculaResponse> minhasMatriculas() {
        UUID usuarioId = UsuarioAutenticado.obrigatorio().id();
        return matriculas.listarDoUsuario(usuarioId).stream()
                .map(m -> {
                    Curso curso = cursos.findById(m.getCursoId()).orElse(null);
                    return new MatriculaResponse(
                            m.getCursoId(),
                            curso == null ? "(curso removido)" : curso.getTitulo(),
                            curso == null ? null : curso.getSlug(),
                            m.getStatus().name(),
                            m.getIniciaEm(),
                            m.getExpiraEm(),
                            // Negativo quando ja venceu -- o frontend usa o sinal para
                            // decidir entre contagem regressiva e oferta de renovacao.
                            Duration.between(Instant.now(), m.getExpiraEm()).toDays());
                })
                .toList();
    }

    /**
     * Secao 10 -- protecao de conteudo.
     *
     * <p>Devolve um token de curta duracao e os dados da marca d'agua. O
     * panda_video_id nao sai daqui sem matricula viva: e o unico ponto onde o
     * acesso pago vira acesso tecnico ao video.
     */
    @GetMapping("/aulas/{id}/player")
    @Operation(summary = "Token de player expiravel, com marca d'agua identificando o aluno")
    public Map<String, Object> tokenDoPlayer(@PathVariable UUID id) {
        UsuarioAutenticado autenticado = UsuarioAutenticado.obrigatorio();
        Usuario usuario = usuarios.buscarAtivoPorId(autenticado.id())
                .orElseThrow(() -> ProblemaNegocio.naoEncontrado("Usuario"));
        return player.autorizar(usuario, id);
    }

    /** Secao 09 -- "progresso automatico" e "continuar assistindo". */
    @PutMapping("/aulas/{id}/progresso")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marca segundos vistos da aula")
    public void marcarProgresso(@PathVariable UUID id, @Valid @RequestBody ProgressoRequest req) {
        UUID usuarioId = UsuarioAutenticado.obrigatorio().id();
        player.exigirAcessoAAula(usuarioId, id);
        progressos.registrar(usuarioId, id, req.segundosVistos(), req.concluido());
    }

    public record ProgressoRequest(
            @Min(value = 0, message = "segundos_vistos nao pode ser negativo") int segundosVistos,
            boolean concluido) {}
}
