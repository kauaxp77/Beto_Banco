package com.betobanco.catalog.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato que o modulo {@code catalog} publica. O modulo {@code payments}
 * precisa resolver um SKU vindo do gateway em um produto, e nada alem disso.
 */
public interface ProductCatalog {

    Optional<ProductSummary> buscarPorSku(String sku);

    Optional<ProductSummary> buscarPorId(UUID id);

    record ProductSummary(UUID id, String sku, String name, long priceCents, boolean active) {
    }
}
