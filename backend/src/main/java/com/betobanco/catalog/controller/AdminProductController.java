package com.betobanco.catalog.controller;

import com.betobanco.catalog.dto.ProductAdminResponse;
import com.betobanco.catalog.dto.ProductCreateRequest;
import com.betobanco.catalog.dto.ProductUpdateRequest;
import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gestao do catalogo. Nao existe DELETE de proposito: pagamentos e
 * entitlements referenciam o produto para sempre — desativar e o unico
 * jeito seguro de tirar algo de venda.
 */
@RestController
@RequestMapping("/admin/products")
@Tag(name = "Admin - Products")
public class AdminProductController {

    private final ProductRepository produtos;

    public AdminProductController(ProductRepository produtos) {
        this.produtos = produtos;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductAdminResponse>>> listar() {
        List<ProductAdminResponse> todos = produtos.findAll(Sort.by("name")).stream()
                .map(ProductAdminResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(todos));
    }

    /**
     * Sem {@code @Transactional} de proposito: a colisao de SKU precisa
     * estourar na transacao do proprio repositorio para que o catch consiga
     * responder 409 em vez de morrer num commit ja marcado para rollback.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductAdminResponse>> criar(
            @Valid @RequestBody ProductCreateRequest req) {
        if (produtos.findBySku(req.sku().trim()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Já existe produto com este SKU");
        }

        try {
            Product criado = produtos.saveAndFlush(new Product(
                    req.sku().trim(), req.name().trim(), req.description(), req.priceCents()));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(ProductAdminResponse.from(criado)));
        } catch (DataIntegrityViolationException e) {
            // Corrida: dois admins criando o mesmo SKU ao mesmo tempo.
            throw new BusinessException(ErrorCode.CONFLICT, "Já existe produto com este SKU");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductAdminResponse>> atualizar(
            @PathVariable("id") UUID id, @Valid @RequestBody ProductUpdateRequest req) {
        Product produto = produtos.findById(id)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        produto.setName(req.name().trim());
        produto.setDescription(req.description());
        produto.setPriceCents(req.priceCents());
        produto.setActive(req.active());
        produtos.saveAndFlush(produto);

        return ResponseEntity.ok(ApiResponse.ok(ProductAdminResponse.from(produto)));
    }
}
