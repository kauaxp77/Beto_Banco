package com.betobanco.catalog.service;

import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProductCatalogService implements ProductCatalog {

    private final ProductRepository produtos;

    public ProductCatalogService(ProductRepository produtos) {
        this.produtos = produtos;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSummary> buscarPorSku(String sku) {
        return produtos.findBySku(sku == null ? "" : sku.trim())
                .map(ProductCatalogService::resumir);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSummary> buscarPorId(UUID id) {
        return produtos.findById(id).map(ProductCatalogService::resumir);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarAtivos() {
        return produtos.countByActiveTrue();
    }

    private static ProductSummary resumir(Product p) {
        return new ProductSummary(p.getId(), p.getSku(), p.getName(), p.getPriceCents(),
                p.isActive());
    }
}
