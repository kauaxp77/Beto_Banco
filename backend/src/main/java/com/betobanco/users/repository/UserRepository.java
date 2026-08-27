package com.betobanco.users.repository;

import com.betobanco.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Busca da tela de alunos do admin. Filtros vem como string vazia (nunca
     * null) para nao depender de inferencia de tipo de parametro nulo no
     * driver do Postgres.
     */
    @Query("""
            select distinct u from User u join u.roles r
            where r.name = 'ROLE_STUDENT'
              and (lower(u.email) like lower(concat('%', :search, '%'))
                   or lower(u.fullName) like lower(concat('%', :search, '%')))
              and (:status = '' or u.status = :status)
            """)
    Page<User> buscarAlunos(@Param("search") String search, @Param("status") String status,
                            Pageable pageable);
}
