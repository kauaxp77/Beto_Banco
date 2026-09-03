package br.com.aprovacao.catalogo;

import br.com.aprovacao.common.Dinheiro;
import br.com.aprovacao.common.PaginaCursor;
import br.com.aprovacao.common.ProblemaNegocio;
import br.com.aprovacao.config.FiltroTenant;
import br.com.aprovacao.config.PropriedadesPlataforma;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secao 19 -- rotas publicas de catalogo, e secao 11 -- sistema de concursos.
 *
 * <p>Secao 11 traz um requisito que nao e cosmetico: "Toda ficha exibe 'verificado
 * em DD/MM/AAAA' e link para a fonte oficial". Salario e vaga errados geram
 * reclamacao e perda de confianca, entao verificado_em e fonte_url saem em toda
 * resposta de concurso, inclusive na listagem.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalogo", description = "Carreiras, concursos e cursos (secoes 07, 11 e 19)")
public class ControladorCatalogo {

    private final EntityManager em;
    private final CursoRepository cursos;
    private final PropriedadesPlataforma props;

    public ControladorCatalogo(EntityManager em, CursoRepository cursos, PropriedadesPlataforma props) {
        this.em = em;
        this.cursos = cursos;
        this.props = props;
    }

    @GetMapping("/carreiras")
    @SecurityRequirements
    @Operation(summary = "Carreiras ativas, na ordem definida pelo admin")
    public List<Map<String, Object>> carreiras() {
        return linhas("""
                SELECT id, nome, slug, descricao, ordem
                  FROM carreira
                 WHERE tenant_id = :tenantId AND ativo
                 ORDER BY ordem, nome
                """, Map.of("tenantId", tenant()), "id", "nome", "slug", "descricao", "ordem");
    }

    /** Secao 19 -- GET /concursos com filtro por carreira, orgao, banca e status. */
    @GetMapping("/concursos")
    @SecurityRequirements
    @Operation(summary = "Lista concursos com filtros")
    public PaginaCursor<Map<String, Object>> concursos(
            @RequestParam(required = false) UUID carreira,
            @RequestParam(required = false) UUID orgao,
            @RequestParam(required = false) String banca,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {

        int limite = PaginaCursor.normalizarLimite(limit);
        String desde = PaginaCursor.decodificar(cursor);

        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("tenantId", tenant());
        parametros.put("carreira", carreira);
        parametros.put("orgao", orgao);
        parametros.put("banca", banca);
        parametros.put("status", status);
        parametros.put("cursorNome", desde);
        parametros.put("limite", limite + 1);

        List<Map<String, Object>> pagina = linhas("""
                SELECT DISTINCT c.id, c.nome, c.slug, c.banca, c.status, c.vagas,
                       c.salario_centavos, c.inscricao_inicio, c.inscricao_fim, c.data_prova,
                       c.pdf_url, c.fonte_url, c.verificado_em, o.sigla
                  FROM concurso c
                  JOIN orgao o ON o.id = c.orgao_id
                  LEFT JOIN concurso_carreira cc ON cc.concurso_id = c.id
                 WHERE c.tenant_id = :tenantId
                   AND (CAST(:carreira AS uuid) IS NULL OR cc.carreira_id = :carreira)
                   AND (CAST(:orgao AS uuid) IS NULL OR c.orgao_id = :orgao)
                   AND (CAST(:banca AS text) IS NULL OR c.banca ILIKE :banca)
                   AND (CAST(:status AS text) IS NULL OR c.status = :status)
                   AND (CAST(:cursorNome AS text) IS NULL OR c.nome > :cursorNome)
                 ORDER BY c.nome
                 LIMIT :limite
                """, parametros,
                "id", "nome", "slug", "banca", "status", "vagas", "salario_centavos",
                "inscricao_inicio", "inscricao_fim", "data_prova",
                "pdf_url", "fonte_url", "verificado_em", "orgao_sigla");

        pagina.forEach(this::anotarVerificacao);
        return PaginaCursor.de(pagina, limite, item -> String.valueOf(item.get("nome")));
    }

    @GetMapping("/concursos/{slug}")
    @SecurityRequirements
    @Operation(summary = "Ficha completa e indexavel do concurso")
    public Map<String, Object> concurso(@PathVariable String slug) {
        List<Map<String, Object>> achados = linhas("""
                SELECT c.id, c.nome, c.slug, c.banca, c.status, c.vagas, c.cadastro_reserva,
                       c.salario_centavos, c.escolaridade, c.beneficios,
                       c.inscricao_inicio, c.inscricao_fim, c.taxa_centavos, c.data_prova,
                       c.pdf_url, c.fonte_url, c.verificado_em, o.nome, o.sigla, o.esfera, o.uf
                  FROM concurso c JOIN orgao o ON o.id = c.orgao_id
                 WHERE c.tenant_id = :tenantId AND c.slug = :slug
                """, Map.of("tenantId", tenant(), "slug", slug),
                "id", "nome", "slug", "banca", "status", "vagas", "cadastro_reserva",
                "salario_centavos", "escolaridade", "beneficios",
                "inscricao_inicio", "inscricao_fim", "taxa_centavos", "data_prova",
                "pdf_url", "fonte_url", "verificado_em",
                "orgao_nome", "orgao_sigla", "orgao_esfera", "orgao_uf");

        if (achados.isEmpty()) {
            throw ProblemaNegocio.naoEncontrado("Concurso");
        }
        Map<String, Object> ficha = achados.get(0);
        anotarVerificacao(ficha);
        return ficha;
    }

    @GetMapping("/cursos")
    @SecurityRequirements
    @Operation(summary = "Cursos publicados")
    public PaginaCursor<Map<String, Object>> cursosPublicados(
            @RequestParam(required = false) UUID carreira,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {

        int limite = PaginaCursor.normalizarLimite(limit);
        String desde = PaginaCursor.decodificar(cursor);
        Instant cursorPublicado = desde == null ? null : Instant.parse(desde);

        List<Map<String, Object>> itens = new ArrayList<>(
                cursos.listarPublicados(tenant(), carreira, cursorPublicado, Limit.of(limite + 1))
                        .stream().map(this::resumoDoCurso).toList());

        return PaginaCursor.de(itens, limite, item -> String.valueOf(item.get("publicado_em")));
    }

    /** Secao 08 -- "Preco visivel, sem 'consulte'". */
    @GetMapping("/cursos/{slug}")
    @SecurityRequirements
    @Operation(summary = "Pagina de venda do curso")
    public Map<String, Object> curso(@PathVariable String slug) {
        Curso curso = cursos.findByTenantIdAndSlug(tenant(), slug)
                .filter(Curso::estaPublicado)
                .orElseThrow(() -> ProblemaNegocio.naoEncontrado("Curso"));

        Map<String, Object> resposta = resumoDoCurso(curso);
        resposta.put("descricao", curso.getDescricao());
        resposta.put("modulos", linhas("""
                SELECT m.id, m.titulo, m.ordem,
                       (SELECT count(*) FROM aula a WHERE a.modulo_id = m.id AND a.publicado_em IS NOT NULL),
                       (SELECT COALESCE(sum(a.duracao_seg), 0) FROM aula a
                         WHERE a.modulo_id = m.id AND a.publicado_em IS NOT NULL)
                  FROM modulo m
                 WHERE m.curso_id = :cursoId
                 ORDER BY m.ordem
                """, Map.of("cursoId", curso.getId()),
                "id", "titulo", "ordem", "aulas", "duracao_seg"));
        return resposta;
    }

    private Map<String, Object> resumoDoCurso(Curso c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("titulo", c.getTitulo());
        m.put("slug", c.getSlug());
        m.put("subtitulo", c.getSubtitulo());
        m.put("capa_storage_key", c.getCapaStorageKey());
        m.put("preco_centavos", c.getPrecoCentavos());
        m.put("preco_formatado", Dinheiro.formatar(c.getPrecoCentavos()));
        m.put("dias_acesso", c.getDiasAcesso());
        m.put("carreira_id", c.getCarreiraId());
        m.put("publicado_em", c.getPublicadoEm());
        return m;
    }

    /**
     * Secao 11 -- ficha sem verificacao ha mais de 60 dias vai para a fila de
     * revisao. O campo sai tambem na API publica para que a pagina possa avisar o
     * aluno de que o dado pode estar defasado, em vez de apresenta-lo como certo.
     */
    private void anotarVerificacao(Map<String, Object> ficha) {
        Object verificado = ficha.get("verificado_em");
        boolean defasada = verificado == null;
        if (verificado instanceof Instant i) {
            defasada = i.isBefore(Instant.now().minusSeconds(60L * 86400));
        } else if (verificado instanceof java.sql.Timestamp t) {
            defasada = t.toInstant().isBefore(Instant.now().minusSeconds(60L * 86400));
        }
        ficha.put("verificacao_defasada", defasada);
    }

    private UUID tenant() {
        return FiltroTenant.atual(props.tenantPadrao());
    }

    private List<Map<String, Object>> linhas(String sql, Map<String, Object> parametros, String... colunas) {
        var consulta = em.createNativeQuery(sql);
        parametros.forEach(consulta::setParameter);
        @SuppressWarnings("unchecked")
        List<Object[]> resultado = consulta.getResultList();

        List<Map<String, Object>> saida = new ArrayList<>(resultado.size());
        for (Object[] linha : resultado) {
            Map<String, Object> registro = new LinkedHashMap<>();
            for (int i = 0; i < colunas.length && i < linha.length; i++) {
                Object valor = linha[i];
                if (valor instanceof java.sql.Date d) {
                    valor = d.toLocalDate();
                } else if (valor instanceof java.sql.Timestamp t) {
                    valor = t.toInstant();
                }
                registro.put(colunas[i], valor);
            }
            saida.add(registro);
        }
        return saida;
    }
}
