package com.betobanco;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationBootTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextoSobeComABeanPrincipal() {
        assertThat(context.getBean(BetoBancoApplication.class)).isNotNull();
    }
}
