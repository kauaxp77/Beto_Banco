package br.com.aprovacao.conteudo;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 20 e 21 -- toda acao de Admin e Suporte grava em log_auditoria.
 *
 * <p>REQUIRES_NEW de proposito: o registro precisa sobreviver ao rollback da
 * operacao auditada. Uma tentativa de estorno que falhou e exatamente o que se
 * quer ver no log depois.
 */
@Service
public class ServicoAuditoria {

    private final LogAuditoriaRepository repositorio;

    public ServicoAuditoria(LogAuditoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(UUID tenantId, UUID usuarioId, String acao,
                          String entidade, String entidadeId, String ip, String userAgent) {
        registrar(tenantId, usuarioId, acao, entidade, entidadeId, "{}", ip, userAgent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(UUID tenantId, UUID usuarioId, String acao, String entidade,
                          String entidadeId, String dadosJson, String ip, String userAgent) {
        repositorio.save(new LogAuditoria(tenantId, usuarioId, acao, entidade, entidadeId, dadosJson, ip, userAgent));
    }
}
