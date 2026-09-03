package br.com.aprovacao.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** Conta viva apenas: a exclusao logica libera o e-mail para novo cadastro. */
    @Query("""
           SELECT u FROM Usuario u
            WHERE lower(u.email) = lower(:email)
              AND u.tenantId = :tenantId
              AND u.excluidoEm IS NULL
           """)
    Optional<Usuario> buscarAtivoPorEmail(@Param("email") String email, @Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM Usuario u WHERE u.id = :id AND u.excluidoEm IS NULL")
    Optional<Usuario> buscarAtivoPorId(@Param("id") UUID id);
}
