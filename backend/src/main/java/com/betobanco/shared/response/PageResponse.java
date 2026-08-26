package com.betobanco.shared.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(boolean success, List<T> data, PaginationMeta pagination) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                true,
                page.getContent(),
                new PaginationMeta(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()));
    }
}
