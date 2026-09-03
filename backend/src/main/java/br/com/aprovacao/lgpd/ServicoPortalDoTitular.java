package br.com.aprovacao.lgpd;

import br.com.aprovacao.auth.SessaoRepository;
import br.com.aprovacao.auth.Usuario;
import br.com.aprovacao.auth.UsuarioRepository;
import br.com.aprovacao.common.ProblemaNegocio;
import br.com.aprovacao.conteudo.ServicoAuditoria;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Secao 22 -- execucao dos direitos do titular. */
@Service
public class ServicoPortalDoTitular {

    private static final Logger log = LoggerFactory.getLogger(ServicoPortalDoTitular.class);

    private final EntityManager em;
    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final ServicoAuditoria auditoria;

    public ServicoPortalDoTitular(EntityManager em, UsuarioRepository usuarios,
                                  SessaoRepository sessoes, ServicoAuditoria auditoria) {
        this.em = em;
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> documentoVigente(UUID tenantId, String tipo) {
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = em.createNativeQuery("""
                SELECT versao, corpo, vigente_de FROM documento_legal
                 WHERE tenant_id = :tenantId AND tipo = :tipo
                 ORDER BY vigente_de DESC
                 LIMIT 1
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("tipo", tipo)
                .getResultList();

        if (linhas.isEmpty()) {
            throw ProblemaNegocio.naoEncontrado("Documento " + tipo);
        }
        Object[] l = linhas.get(0);
        return Map.of("tipo", tipo, "versao", l[0], "corpo", l[1], "vigente_de", l[2]);
    }

    /**
     * Exportacao em JSON de tudo que a plataforma guarda sobre o titular.
     *
     * <p>A lista de tabelas abaixo espelha a tabela de retencao da secao 22. Toda
     * entidade nova que guardar dado pessoal precisa entrar aqui -- exportacao
     * incompleta e descumprimento, nao apenas um recurso faltando.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportar(UUID usuarioId) {
        Usuario usuario = usuarios.buscarAtivoPorId(usuarioId)
                .orElseThrow(() -> ProblemaNegocio.naoEncontrado("Usuario"));

        Map<String, Object> cadastro = new LinkedHashMap<>();
        cadastro.put("nome", usuario.getNome());
        cadastro.put("email", usuario.getEmail());
        cadastro.put("cpf", usuario.getCpf());
        cadastro.put("whatsapp", usuario.getWhatsapp());
        cadastro.put("data_nascimento", usuario.getDataNascimento());
        cadastro.put("criado_em", usuario.getCriadoEm());
        cadastro.put("ultimo_acesso_em", usuario.getUltimoAcessoEm());

        Map<String, Object> pacote = new LinkedHashMap<>();
        pacote.put("gerado_em", Instant.now());
        pacote.put("cadastro", cadastro);
        pacote.put("matriculas", consultar("""
                SELECT m.curso_id, c.titulo, m.status, m.inicia_em, m.expira_em
                  FROM matricula m LEFT JOIN curso c ON c.id = m.curso_id
                 WHERE m.usuario_id = :id
                """, usuarioId, "curso_id", "curso", "status", "inicia_em", "expira_em"));
        pacote.put("pedidos", consultar("""
                SELECT id, valor_centavos, desconto_centavos, status, criado_em
                  FROM pedido WHERE usuario_id = :id
                """, usuarioId, "id", "valor_centavos", "desconto_centavos", "status", "criado_em"));
        pacote.put("progresso", consultar("""
                SELECT aula_id, segundos_vistos, concluido_em
                  FROM progresso_aula WHERE usuario_id = :id
                """, usuarioId, "aula_id", "segundos_vistos", "concluido_em"));
        pacote.put("tentativas_de_simulado", consultar("""
                SELECT simulado_id, iniciada_em, enviada_em, nota
                  FROM tentativa WHERE usuario_id = :id
                """, usuarioId, "simulado_id", "iniciada_em", "enviada_em", "nota"));
        pacote.put("redacoes", consultar("""
                SELECT id, tema, status, enviada_em FROM redacao WHERE usuario_id = :id
                """, usuarioId, "id", "tema", "status", "enviada_em"));
        pacote.put("consentimentos", consultar("""
                SELECT finalidade, concedido, texto_aceito, registrado_em
                  FROM consentimento WHERE usuario_id = :id ORDER BY registrado_em DESC
                """, usuarioId, "finalidade", "concedido", "texto_aceito", "registrado_em"));
        pacote.put("sessoes", consultar("""
                SELECT dispositivo, ip, criado_em, expira_em, revogado_em
                  FROM sessao WHERE usuario_id = :id
                """, usuarioId, "dispositivo", "ip", "criado_em", "expira_em", "revogado_em"));

        auditoria.registrar(usuario.getTenantId(), usuarioId, "EXPORTACAO_DE_DADOS",
                "usuario", usuarioId.toString(), null, null);
        return pacote;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> consentimentosVigentes(UUID usuarioId) {
        return consultar("""
                SELECT DISTINCT ON (finalidade) finalidade, concedido, registrado_em
                  FROM consentimento
                 WHERE usuario_id = :id
                 ORDER BY finalidade, registrado_em DESC
                """, usuarioId, "finalidade", "concedido", "registrado_em");
    }

    /**
     * Exclusao: anonimiza o cadastro, apaga o que e puramente pessoal e mantem o
     * pedido, que e obrigacao fiscal de 5 anos. A auditoria e gravada antes da
     * anonimizacao, com o id -- que continua existindo -- e nao com o e-mail.
     */
    @Transactional
    public void excluirConta(UUID usuarioId) {
        Usuario usuario = usuarios.buscarAtivoPorId(usuarioId)
                .orElseThrow(() -> ProblemaNegocio.naoEncontrado("Usuario"));

        auditoria.registrar(usuario.getTenantId(), usuarioId, "EXCLUSAO_DE_CONTA",
                "usuario", usuarioId.toString(), null, null);

        sessoes.revogarTodasDoUsuario(usuarioId, Instant.now());
        em.createNativeQuery("DELETE FROM acesso_ip WHERE usuario_id = :id")
                .setParameter("id", usuarioId).executeUpdate();
        em.createNativeQuery("DELETE FROM favorito WHERE usuario_id = :id")
                .setParameter("id", usuarioId).executeUpdate();

        // O pedido perde o vinculo com a pessoa mas continua existindo para o fisco.
        em.createNativeQuery("""
                UPDATE pedido SET nome = NULL, whatsapp = NULL, cpf = NULL,
                                  email = 'removido+' || id || '@invalido.local'
                 WHERE usuario_id = :id
                """).setParameter("id", usuarioId).executeUpdate();

        usuario.anonimizar();
        usuarios.save(usuario);
        log.info("Conta {} anonimizada a pedido do titular. Registro fiscal preservado.", usuarioId);
    }

    private List<Map<String, Object>> consultar(String sql, UUID usuarioId, String... colunas) {
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = em.createNativeQuery(sql).setParameter("id", usuarioId).getResultList();
        List<Map<String, Object>> saida = new ArrayList<>(linhas.size());
        for (Object[] linha : linhas) {
            Map<String, Object> registro = new LinkedHashMap<>();
            for (int i = 0; i < colunas.length && i < linha.length; i++) {
                registro.put(colunas[i], linha[i]);
            }
            saida.add(registro);
        }
        return saida;
    }
}
