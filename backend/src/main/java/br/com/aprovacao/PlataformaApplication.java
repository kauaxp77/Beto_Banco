package br.com.aprovacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
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
/*
 * UserDetailsServiceAutoConfiguration fica de fora: sem ela o Spring Boot criava
 * um usuario "user" em memoria com senha aleatoria e a imprimia no log a cada
 * subida. Essa conta nao e alcancavel -- a cadeia de filtros nao tem form login
 * nem HTTP Basic --, mas um bean de autenticacao que ninguem pediu e uma linha
 * de credencial no log de producao sao duas coisas que nao deveriam existir.
 * A autenticacao real e a da secao 20, em ServicoAutenticacao e FiltroJwt.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class PlataformaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlataformaApplication.class, args);
    }
}
