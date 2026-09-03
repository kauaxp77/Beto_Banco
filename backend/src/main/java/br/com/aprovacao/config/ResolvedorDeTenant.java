package br.com.aprovacao.config;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 27: "Dominio proprio do cliente via CNAME, com SSL automatico."
 *
 * <p>O mapa dominio -> tenant muda raramente e e lido em toda requisicao anonima;
 * o cache em memoria evita uma consulta por pageview. Como a Fase 5 e quem cria
 * tenants, invalidar o cache no cadastro de dominio basta -- por isso o metodo
 * publico {@link #esquecer(String)}.
 */
@Component
public class ResolvedorDeTenant {

    private final EntityManager em;
    private final Map<String, UUID> cache = new ConcurrentHashMap<>();

    public ResolvedorDeTenant(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public UUID porDominio(String dominio) {
        if (dominio == null || dominio.isBlank()) {
            return null;
        }
        return cache.computeIfAbsent(dominio.toLowerCase(), d -> {
            var resultado = em.createNativeQuery("SELECT id FROM tenant WHERE lower(dominio) = :d AND ativo")
                    .setParameter("d", d)
                    .getResultList();
            return resultado.isEmpty() ? null : (UUID) resultado.get(0);
        });
    }

    public void esquecer(String dominio) {
        if (dominio != null) {
            cache.remove(dominio.toLowerCase());
        }
    }
}
