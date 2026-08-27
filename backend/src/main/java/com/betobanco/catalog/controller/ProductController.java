package com.betobanco.catalog.controller;

import com.betobanco.catalog.dto.ProductPublicResponse;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogo publico. So produtos ativos aparecem: desativar um produto o tira
 * da vitrine sem apagar o historico de pagamentos que o referencia.
 */
@RestController
@RequestMapping("/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductRepository produtos;

    public ProductController(ProductRepository produtos) {
        this.produtos = produtos;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ProductPublicResponse>>> listar() {
        List<ProductPublicResponse> ativos = produtos.findByActiveTrueOrderByNameAsc().stream()
                .map(p -> new ProductPublicResponse(p.getId(), p.getSku(), p.getName(),
                        p.getDescription(), p.getPriceCents(), p.getCurrency()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(ativos));
    }
}
