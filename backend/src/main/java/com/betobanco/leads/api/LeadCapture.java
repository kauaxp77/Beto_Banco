package com.betobanco.leads.api;

import java.util.UUID;

/**
 * Contrato que o modulo {@code leads} publica para quem descobre uma venda
 * perdida. Documento Mestre Premium V3.0, secao 8: "Recusado. Criar lead."
 *
 * <p>Existe como porta para que o modulo de pagamentos nao precise conhecer as
 * entidades do CRM: pagamento sabe que uma venda nao se concretizou e quem era
 * o comprador; o que se faz com isso e problema do funil.
 */
public interface LeadCapture {

    /**
     * Registra uma venda que nao se concretizou.
     *
     * <p>A implementacao nunca propaga excecao: perder um lead e ruim, mas
     * derrubar por causa disso o processamento do pagamento que o originou
     * seria pior — o razao financeiro nao pode depender do CRM.
     */
    void registrarVendaPerdida(VendaPerdida venda);

    /** Os campos que a secao 8 manda registrar: nome, e-mail, curso, valor, motivo. */
    record VendaPerdida(String nome, String email, UUID productId, long amountCents,
                        Motivo motivo, String detalhe) {
    }

    /**
     * Cancelado e recusado sao coisas diferentes para quem vai ligar: o cartao
     * recusado costuma ser problema de meio de pagamento e converte com uma
     * segunda tentativa; o cancelamento e uma decisao, e exige outro argumento.
     */
    enum Motivo {
        RECUSADO,
        CANCELADO
    }
}
