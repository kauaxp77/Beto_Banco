package com.betobanco.shared.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class PageRequestFactoryTest {

    @Test
    void usaPadraoQuandoParametrosSaoNulos() {
        Pageable p = PageRequestFactory.of(null, null, null);

        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(20);
    }

    @Test
    void limitaSizeAoTeto() {
        Pageable p = PageRequestFactory.of(0, 100000, null);

        assertThat(p.getPageSize()).isEqualTo(PageRequestFactory.MAX_SIZE);
    }

    @Test
    void rejeitaValoresNegativos() {
        Pageable p = PageRequestFactory.of(-5, -10, null);

        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(20);
    }

    @Test
    void interpretaOrdenacaoDescendente() {
        Pageable p = PageRequestFactory.of(0, 20, "createdAt,desc");

        Sort.Order order = p.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void assumeAscendenteQuandoDirecaoOmitida() {
        Pageable p = PageRequestFactory.of(0, 20, "nome");

        Sort.Order order = p.getSort().getOrderFor("nome");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void naoQuebraComSortSoDeVirgula() {
        Pageable p = PageRequestFactory.of(0, 20, ",");

        assertThat(p.getSort().isSorted()).isFalse();
    }

    @Test
    void tratasizeZeroComoPadrao() {
        Pageable p = PageRequestFactory.of(0, 0, null);

        assertThat(p.getPageSize()).isEqualTo(20);
    }

    @Test
    void reconheceDirecaoEmMaiusculas() {
        Pageable p = PageRequestFactory.of(0, 20, "createdAt,DESC");

        Sort.Order order = p.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
