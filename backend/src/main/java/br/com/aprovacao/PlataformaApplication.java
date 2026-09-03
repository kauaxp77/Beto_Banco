package br.com.aprovacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * API da Plataforma de Mentoria e Concursos.
 *
 * <p>Implementa o Documento Mestre da Plataforma V4.0. Cada regra de negocio nao
 * obvia carrega no codigo a referencia da secao que a originou, para que uma
 * mudanca de escopo seja rastreavel ate o documento que a decidiu.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class PlataformaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlataformaApplication.class, args);
    }
}
