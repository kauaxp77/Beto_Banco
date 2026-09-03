package br.com.aprovacao.conteudo;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findByEntidadeAndEntidadeIdOrderByCriadoEmDesc(String entidade, String entidadeId, Limit limite);
}
