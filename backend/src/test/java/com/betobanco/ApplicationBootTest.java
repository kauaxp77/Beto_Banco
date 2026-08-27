package com.betobanco;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationBootTest extends PostgresTestBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextoSobeComABeanPrincipal() {
        assertThat(context.getBean(BetoBancoApplication.class)).isNotNull();
    }
}
