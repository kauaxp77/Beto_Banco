package com.betobanco.shared.response;

public record PaginationMeta(int page, int size, long totalElements, int totalPages) {
}
