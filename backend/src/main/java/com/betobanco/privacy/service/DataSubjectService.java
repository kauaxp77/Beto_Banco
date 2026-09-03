package com.betobanco.privacy.service;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.privacy.entity.DataSubjectRequest;
import com.betobanco.privacy.repository.DataSubjectRequestRepository;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Portal do titular. Documento Mestre V4.0, secao 22.
 *
 * <p>"Portal do titular no perfil: exportar meus dados (JSON), corrigir,
 * revogar consentimento, excluir conta." O documento e explicito em que isso e
 * autoatendimento na Fase 1, nao pedido por e-mail.
 */
@Service
public class DataSubjectService {

    private static final Logger log = LoggerFactory.getLogger(DataSubjectService.class);

    /**
     * Tudo que guarda dado do titular, com as colunas que saem na exportacao.
     *
     * <p>A lista espelha a tabela de retencao da secao 22. Entidade nova que
     * guarde dado pessoal precisa entrar aqui: exportacao incompleta e
     * descumprimento, nao um recurso faltando — e o jeito de isso passar
     * despercebido e a lista estar espalhada por dez metodos em vez de visivel
     * em um lugar so.
     */
    private static final List<Conjunto> EXPORTAVEIS = List.of(
            new Conjunto("perfil", """
                    SELECT u.email, u.full_name, u.status, u.created_at, s.phone
                      FROM users u LEFT JOIN students s ON s.id = u.id
                     WHERE u.id = :id
                    """, "email", "nome_completo", "status", "criado_em", "telefone"),

            new Conjunto("acessos_liberados", """
                    SELECT p.name, e.source, e.granted_at, e.expires_at, e.revoked_at
                      FROM entitlements e JOIN products p ON p.id = e.product_id
                     WHERE e.user_id = :id ORDER BY e.granted_at DESC
                    """, "produto", "origem", "concedido_em", "expira_em", "revogado_em"),

            new Conjunto("pagamentos", """
                    SELECT id, provider, amount_cents, currency, status, created_at
                      FROM payments WHERE user_id = :id ORDER BY created_at DESC
                    """, "id", "gateway", "valor_centavos", "moeda", "status", "criado_em"),

            new Conjunto("progresso_nas_aulas", """
                    SELECT lesson_id, completed_at
                      FROM lesson_progress WHERE user_id = :id
                    """, "aula_id", "concluida_em"),

            new Conjunto("tentativas_de_questoes", """
                    SELECT lesson_id, correct_count, total_count, answers, created_at
                      FROM quiz_attempts WHERE user_id = :id ORDER BY created_at DESC
                    """, "aula_id", "acertos", "total", "respostas", "respondida_em"),

            new Conjunto("comentarios", """
                    SELECT lesson_id, body, created_at
                      FROM lesson_comments WHERE user_id = :id ORDER BY created_at DESC
                    """, "aula_id", "texto", "criado_em"),

            new Conjunto("avaliacoes_de_aula", """
                    SELECT lesson_id, helpful, created_at
                      FROM lesson_ratings WHERE user_id = :id
                    """, "aula_id", "foi_util", "criado_em"),

            new Conjunto("certificados", """
                    SELECT course_id, code, hours, issued_at FROM certificates WHERE user_id = :id
                    """, "curso_id", "codigo", "horas", "emitido_em"),

            new Conjunto("depoimentos", """
                    SELECT body, status, created_at FROM testimonials WHERE user_id = :id
                    """, "texto", "status", "criado_em"),

            new Conjunto("consentimentos", """
                    SELECT purpose, granted, accepted_text, ip, recorded_at
                      FROM consents WHERE user_id = :id ORDER BY recorded_at DESC
                    """, "finalidade", "concedido", "texto_aceito", "ip", "registrado_em"),

            new Conjunto("aceites_de_termos", """
                    SELECT d.type, d.version, a.ip, a.accepted_at
                      FROM legal_acceptances a JOIN legal_documents d ON d.id = a.document_id
                     WHERE a.user_id = :id ORDER BY a.accepted_at DESC
                    """, "documento", "versao", "ip", "aceito_em"),

            new Conjunto("sessoes", """
                    SELECT ip, user_agent, issued_at, expires_at, revoked_at
                      FROM refresh_tokens WHERE user_id = :id ORDER BY issued_at DESC
                    """, "ip", "dispositivo", "criada_em", "expira_em", "revogada_em"));

    private final EntityManager em;
    private final UserDirectory usuarios;
    private final DataSubjectRequestRepository pedidos;
    private final ConsentService consentimentos;
    private final AuditLogger auditoria;

    public DataSubjectService(EntityManager em, UserDirectory usuarios,
                              DataSubjectRequestRepository pedidos,
                              ConsentService consentimentos, AuditLogger auditoria) {
        this.em = em;
        this.usuarios = usuarios;
        this.pedidos = pedidos;
        this.consentimentos = consentimentos;
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------
    // Portabilidade
    // ------------------------------------------------------------------

    @Transactional
    public Map<String, Object> exportar(UUID userId) {
        UserAccount conta = usuarios.buscarAtivoPorId(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        Map<String, Object> pacote = new LinkedHashMap<>();
        pacote.put("gerado_em", Instant.now());
        pacote.put("titular", conta.email());
        for (Conjunto c : EXPORTAVEIS) {
            pacote.put(c.nome(), consultar(c, userId));
        }

        pedidos.save(new DataSubjectRequest(userId, conta.email(),
                DataSubjectRequest.EXPORT, "Exportação atendida na hora, pelo portal."));
        auditoria.registrar("DATA_EXPORTED", "User", userId.toString(),
                Map.of("conjuntos", EXPORTAVEIS.size()));

        return pacote;
    }

    // ------------------------------------------------------------------
    // Exclusao
    // ------------------------------------------------------------------

    /**
     * Secao 22: "Exclusao anonimiza o cadastro mas preserva o registro fiscal do
     * pedido — obrigacao legal se sobrepoe."
     *
     * <p>Tres tratamentos diferentes, porque as bases legais sao diferentes:
     *
     * <ul>
     *   <li><b>Apagado</b> — o que so existia por execucao de contrato e cuja
     *       retencao acaba com a conta: historico de estudo, comentarios,
     *       avaliacoes, certificados, depoimentos, sessoes.</li>
     *   <li><b>Anonimizado</b> — o que precisa continuar existindo como linha,
     *       mas nao como pessoa: o cadastro e o contato nos pagamentos.</li>
     *   <li><b>Intocado</b> — o que outra lei manda guardar: valores e status
     *       dos pagamentos (prazo fiscal) e o log de auditoria, que a secao 21
     *       exige imutavel por 5 anos. Apagar auditoria a pedido do titular
     *       destruiria justamente a prova de que o pedido foi atendido.</li>
     * </ul>
     */
    @Transactional
    public void excluirConta(UUID userId) {
        UserAccount conta = usuarios.buscarAtivoPorId(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        // Registra antes de anonimizar: depois o e-mail nao existe mais.
        pedidos.save(new DataSubjectRequest(userId, conta.email(),
                DataSubjectRequest.DELETION, "Exclusão solicitada pelo titular no portal."));
        auditoria.registrar("ACCOUNT_ANONYMIZED", "User", userId.toString(),
                Map.of("motivo", "direito de exclusao — secao 22"));

        apagar("DELETE FROM lesson_progress WHERE user_id = :id", userId);
        apagar("DELETE FROM quiz_attempts   WHERE user_id = :id", userId);
        apagar("DELETE FROM lesson_comments WHERE user_id = :id", userId);
        apagar("DELETE FROM lesson_ratings  WHERE user_id = :id", userId);
        apagar("DELETE FROM certificates    WHERE user_id = :id", userId);
        apagar("DELETE FROM testimonials    WHERE user_id = :id", userId);
        apagar("DELETE FROM refresh_tokens  WHERE user_id = :id", userId);
        apagar("DELETE FROM password_reset_tokens WHERE user_id = :id", userId);
        apagar("UPDATE students SET phone = NULL WHERE id = :id", userId);

        // O pagamento sobrevive com valor e status; o contato, nao. Nada em
        // obrigacao fiscal exige guardar o e-mail do comprador.
        em.createNativeQuery("""
                UPDATE payments
                   SET buyer_email = 'anonimizado+' || id || '@invalido.local',
                       buyer_name  = NULL
                 WHERE user_id = :id
                """).setParameter("id", userId).executeUpdate();

        // O e-mail vira um valor irreversivel e unico, para nao colidir com o
        // indice unico nem permitir reconstruir o original.
        em.createNativeQuery("""
                UPDATE users
                   SET email         = 'anonimizado+' || id || '@invalido.local',
                       full_name     = 'Titular removido',
                       status        = 'BLOCKED',
                       anonymized_at = now()
                 WHERE id = :id
                """).setParameter("id", userId).executeUpdate();

        log.info("Conta {} anonimizada a pedido do titular. Registro fiscal preservado.", userId);
    }

    @Transactional
    public int revogarTodosOsConsentimentos(UUID userId, String ip) {
        UserAccount conta = usuarios.buscarAtivoPorId(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        int revogados = consentimentos.revogarTudo(userId, ip);
        pedidos.save(new DataSubjectRequest(userId, conta.email(),
                DataSubjectRequest.CONSENT_WITHDRAWAL,
                "%d consentimento(s) revogado(s) pelo portal.".formatted(revogados)));
        return revogados;
    }

    @Transactional(readOnly = true)
    public List<DataSubjectRequest> pedidosDe(UUID userId) {
        return pedidos.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ------------------------------------------------------------------

    private void apagar(String sql, UUID userId) {
        em.createNativeQuery(sql).setParameter("id", userId).executeUpdate();
    }

    private List<Map<String, Object>> consultar(Conjunto c, UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object> linhas = em.createNativeQuery(c.sql()).setParameter("id", userId).getResultList();

        List<Map<String, Object>> saida = new ArrayList<>(linhas.size());
        for (Object linha : linhas) {
            // Consulta de coluna unica devolve o valor solto, nao um array.
            Object[] valores = linha instanceof Object[] arr ? arr : new Object[] {linha};
            Map<String, Object> registro = new LinkedHashMap<>();
            for (int i = 0; i < c.colunas().size() && i < valores.length; i++) {
                registro.put(c.colunas().get(i), valores[i]);
            }
            saida.add(registro);
        }
        return saida;
    }

    private record Conjunto(String nome, String sql, List<String> colunas) {
        Conjunto(String nome, String sql, String... colunas) {
            this(nome, sql, List.of(colunas));
        }
    }
}
