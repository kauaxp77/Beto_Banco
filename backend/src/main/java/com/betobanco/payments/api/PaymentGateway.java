package com.betobanco.payments.api;

import java.util.Optional;

/**
 * Contrato de um gateway de pagamento.
 *
 * <p>Daqui para dentro do dominio, nenhuma classe conhece o formato de nenhum
 * provedor. Trocar de gateway, ou acrescentar um segundo, e escrever outra
 * implementacao desta interface — a regra de negocio nao muda.
 */
public interface PaymentGateway {

    /** Identificador do provedor, como gravado em {@code payments.provider}. */
    String provider();

    /**
     * Verifica a autenticidade do webhook sobre os BYTES CRUS recebidos.
     *
     * <p>A assinatura HMAC e calculada sobre os bytes exatos que o provedor
     * enviou. Qualquer round-trip por um parser de JSON reordena campos ou
     * muda espacamento e quebra a verificacao de um jeito que parece erro de
     * chave — por isso este metodo recebe {@code byte[]}, e nao um objeto.
     */
    boolean assinaturaValida(byte[] corpoCru, java.util.Map<String, String> cabecalhos);

    /**
     * Traduz o payload do provedor para a forma canonica do sistema.
     * Devolve vazio quando o evento nao e interpretavel.
     */
    Optional<PaymentNotification> interpretar(byte[] corpoCru);
}
