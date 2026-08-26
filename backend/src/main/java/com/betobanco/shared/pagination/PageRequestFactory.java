package com.betobanco.shared.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {

    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    private PageRequestFactory() {
    }

    public static Pageable of(Integer page, Integer size, String sort) {
        int pagina = (page == null || page < 0) ? 0 : page;
        int tamanho = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(pagina, tamanho, parseSort(sort));
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        String[] partes = sort.split(",");
        if (partes.length == 0) {
            return Sort.unsorted();
        }
        String campo = partes[0].trim();
        if (campo.isEmpty()) {
            return Sort.unsorted();
        }
        Sort.Direction direcao = (partes.length > 1 && "desc".equalsIgnoreCase(partes[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direcao, campo);
    }
}
