package br.com.aprovacao.comercial;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CupomRepository extends JpaRepository<Cupom, UUID> {

    @Query("SELECT c FROM Cupom c WHERE c.tenantId = :tenantId AND upper(c.codigo) = upper(:codigo)")
    Optional<Cupom> buscarPorCodigo(@Param("tenantId") UUID tenantId, @Param("codigo") String codigo);
}
