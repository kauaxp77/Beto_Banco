package com.betobanco.audit.repository;

import com.betobanco.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String tipo, String id);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
