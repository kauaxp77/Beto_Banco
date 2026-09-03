package br.com.aprovacao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Secao 19: "OpenAPI gerado pelo springdoc e publicado em /api/docs". */
@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plataforma de Mentoria e Concursos -- API")
                        .version("v1")
                        .description("""
                                Contrato da API conforme o Documento Mestre da Plataforma V4.0, secao 19.

                                Convencoes:
                                - Base /api/v1. Quebra de contrato so em nova versao de caminho.
                                - JSON em snake_case. Datas em ISO 8601 com fuso.
                                - Erro no formato RFC 7807.
                                - Paginacao por cursor: ?limit=20&cursor=..., resposta com next_cursor.
                                - Idempotency-Key obrigatorio em todo POST que cria pedido ou pagamento.
                                - Limite de requisicao: 60/min por IP no publico, 10/min em login e recuperacao de senha.
                                """)
                        .contact(new Contact().name("Product Owner")))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token de 15 minutos emitido por POST /api/v1/auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
