package com.betobanco.catalog.dto;

import java.util.UUID;

/** Vitrine publica: nada aqui alem do que qualquer visitante pode ver. */
public record ProductPublicResponse(
        UUID id,
        String sku,
        String name,
        String description,
        long priceCents,
        String currency) {
}
