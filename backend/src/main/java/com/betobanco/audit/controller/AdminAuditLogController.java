package com.betobanco.audit.controller;

import com.betobanco.audit.dto.AuditLogResponse;
import com.betobanco.audit.repository.AuditLogRepository;
import com.betobanco.shared.pagination.PageRequestFactory;
import com.betobanco.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Auditoria e somente-leitura por definicao: nao existe POST aqui. */
@RestController
@RequestMapping("/admin/audit-logs")
@Tag(name = "Admin - Audit")
public class AdminAuditLogController {

    private final AuditLogRepository logs;

    public AdminAuditLogController(AuditLogRepository logs) {
        this.logs = logs;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<AuditLogResponse>> listar(
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        var paginacao = PageRequestFactory.of(page, size, null);
        var pagina = (action == null || action.isBlank())
                ? logs.findAllByOrderByCreatedAtDesc(paginacao)
                : logs.findByActionOrderByCreatedAtDesc(action.trim(), paginacao);

        return ResponseEntity.ok(PageResponse.from(pagina.map(AuditLogResponse::from)));
    }
}
