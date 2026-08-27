package com.betobanco.payments.controller;

import com.betobanco.payments.dto.PaymentAdminResponse;
import com.betobanco.payments.repository.PaymentRepository;
import com.betobanco.shared.pagination.PageRequestFactory;
import com.betobanco.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de pagamentos pelo admin — somente leitura, o gateway e a fonte. */
@RestController
@RequestMapping("/admin/payments")
@Tag(name = "Admin - Payments")
public class AdminPaymentController {

    private final PaymentRepository pagamentos;

    public AdminPaymentController(PaymentRepository pagamentos) {
        this.pagamentos = pagamentos;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<PaymentAdminResponse>> listar(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        Pageable paginacao = PageRequestFactory.of(page, size, null);
        var pagina = (status == null || status.isBlank())
                ? pagamentos.findAllByOrderByCreatedAtDesc(paginacao)
                : pagamentos.findByStatusOrderByCreatedAtDesc(status.trim(), paginacao);

        return ResponseEntity.ok(PageResponse.from(pagina.map(PaymentAdminResponse::from)));
    }
}
