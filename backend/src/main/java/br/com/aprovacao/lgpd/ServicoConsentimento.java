package br.com.aprovacao.lgpd;

import br.com.aprovacao.comercial.Pedido;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 22 -- LGPD.
 *
 * <p>"Politica de privacidade e termos de uso versionados, com aceite registrado
 * (data, hora, IP, versao)." O aceite aponta para a linha exata de
 * documento_legal: dizer "aceitou os termos" sem saber qual texto estava no ar
 * naquele dia nao serve como prova.
 *
 * <p>"Caixa pre-marcada nao e consentimento valido" (secao 16): por isso o
 * consentimento de marketing e sempre um booleano vindo do cliente, e a ausencia
 * do campo grava recusa, nunca aceite.
 */
@Service
public class ServicoConsentimento {

    private static final Logger log = LoggerFactory.getLogger(ServicoConsentimento.class);

    private final EntityManager em;

    public ServicoConsentimento(EntityManager em) {
        this.em = em;
    }

    @Transactional
    public void registrarAceiteDeCompra(UUID tenantId, Pedido pedido, String versao, String ip, String userAgent) {
        UUID documentoId = idDoDocumento(tenantId, "TERMOS_DE_USO", versao);
        if (documentoId == null) {
            // Nao bloqueamos a compra por causa disso -- mas o alerta precisa existir,
            // porque um aceite sem documento correspondente e uma lacuna juridica.
            log.error("Aceite de termos versao '{}' sem documento correspondente no tenant {}.", versao, tenantId);
            return;
        }
        em.createNativeQuery("""
                INSERT INTO aceite_legal (usuario_id, documento_id, ip, user_agent)
                VALUES (:usuarioId, :documentoId, :ip, :userAgent)
                """)
                .setParameter("usuarioId", pedido.getUsuarioId())
                .setParameter("documentoId", documentoId)
                .setParameter("ip", ip)
                .setParameter("userAgent", userAgent)
                .executeUpdate();
    }

    /**
     * Secao 22: "Banner de cookies com recusa tao facil quanto o aceite. Analytics
     * so dispara apos consentimento."
     */
    @Transactional
    public void registrarConsentimento(UUID usuarioId, String finalidade, boolean concedido,
                                       String textoAceito, String ip) {
        em.createNativeQuery("""
                INSERT INTO consentimento (usuario_id, finalidade, concedido, texto_aceito, ip)
                VALUES (:usuarioId, :finalidade, :concedido, :texto, :ip)
                """)
                .setParameter("usuarioId", usuarioId)
                .setParameter("finalidade", finalidade)
                .setParameter("concedido", concedido)
                .setParameter("texto", textoAceito)
                .setParameter("ip", ip)
                .executeUpdate();
    }

    /** Consulta o consentimento vigente: vale sempre o registro mais recente. */
    @Transactional(readOnly = true)
    public boolean temConsentimento(UUID usuarioId, String finalidade) {
        var resultado = em.createNativeQuery("""
                SELECT concedido FROM consentimento
                 WHERE usuario_id = :usuarioId AND finalidade = :finalidade
                 ORDER BY registrado_em DESC
                 LIMIT 1
                """)
                .setParameter("usuarioId", usuarioId)
                .setParameter("finalidade", finalidade)
                .getResultList();
        return !resultado.isEmpty() && Boolean.TRUE.equals(resultado.get(0));
    }

    private UUID idDoDocumento(UUID tenantId, String tipo, String versao) {
        var resultado = em.createNativeQuery("""
                SELECT id FROM documento_legal
                 WHERE tenant_id = :tenantId AND tipo = :tipo AND versao = :versao
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("tipo", tipo)
                .setParameter("versao", versao)
                .getResultList();
        return resultado.isEmpty() ? null : (UUID) resultado.get(0);
    }
}
