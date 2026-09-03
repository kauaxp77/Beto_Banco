package br.com.aprovacao.config;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Toda constante de negocio que o Documento Mestre fixa vive aqui, nao espalhada
 * em campos estaticos. Mudar o prazo de arrependimento ou a cota de redacao vira
 * uma linha de configuracao, nao um deploy de codigo.
 */
@ConfigurationProperties(prefix = "plataforma")
public record PropriedadesPlataforma(
        UUID tenantPadrao,
        Jwt jwt,
        Seguranca seguranca,
        RateLimit rateLimit,
        Pagamento pagamento,
        Acesso acesso,
        Reembolso reembolso,
        Redacao redacao,
        Storage storage,
        Ia ia) {

    /** Secao 20 -- access de 15 min, refresh de 30 dias com rotacao. */
    public record Jwt(String segredo, String emissor, int accessMinutos, int refreshDias) {}

    /** Secao 21 -- senha, bloqueio por tentativa, CORS e 2FA. */
    public record Seguranca(
            int maxFalhasLogin,
            int bloqueioMinutos,
            int senhaMinima,
            List<String> corsOrigens,
            List<String> mfaObrigatorioPerfis) {}

    /** Secao 19 -- limite de requisicao. */
    public record RateLimit(int publicoPorMinuto, int autenticacaoPorMinuto) {}

    /** Secao 12 -- gateway, contrato do webhook e expiracao do pedido. */
    public record Pagamento(
            String gateway,
            String webhookSegredo,
            String apiUrl,
            String apiToken,
            List<Integer> backoffMinutos,
            int pedidoExpiraHoras) {}

    /** Secao 10 -- protecao de conteudo. */
    public record Acesso(int maxSessoesSimultaneas, int alertaIpsDistintos24h, int urlAssinadaSegundos) {}

    /** Secao 12 -- CDC art. 49 e garantia comercial. */
    public record Reembolso(int arrependimentoDias, int garantiaComercialDias, int garantiaConsumoMaximoPercentual) {}

    /** Secao 14 -- prazo, tamanho e cota de correcao. */
    public record Redacao(int prazoDias, int tamanhoMaximoMb, int cotaMentoriaMes) {}

    /** Secao 10 / 23 -- Cloudflare R2. */
    public record Storage(String endpoint, String bucket, String accessKey, String secretKey) {}

    /** Secao 17 -- teto de gasto por aluno e por mes, com alerta em 80%. */
    public record Ia(String modelo, String apiKey, long tetoCentavosAlunoMes, int alertaPercentual) {}
}
