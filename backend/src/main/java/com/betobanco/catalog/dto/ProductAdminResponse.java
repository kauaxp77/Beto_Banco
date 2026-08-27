package com.betobanco.catalog.dto;

import com.betobanco.catalog.entity.Product;

import java.util.UUID;

/** Visao do admin: inclui {@code active}, que a vitrine publica omite. */
public record ProductAdminResponse(
        UUID id,
        String sku,
        String name,
        String description,
        long priceCents,
        String currency,
        boolean active) {

    public static ProductAdminResponse from(Product p) {
        return new ProductAdminResponse(p.getId(), p.getSku(), p.getName(),
                p.getDescription(), p.getPriceCents(), p.getCurrency(), p.isActive());
    }
}
