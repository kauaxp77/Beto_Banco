package br.com.aprovacao.pagamento;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Secao 12: "Assinatura: validar HMAC do cabecalho antes de processar. Assinatura
 * invalida devolve 401 e nao grava nada."
 */
public final class AssinaturaHmac {

    private AssinaturaHmac() {}

    /**
     * @param corpoBruto bytes exatos recebidos. Reserializar o JSON antes de
     *                   assinar muda espacos e ordem de chave, e a assinatura
     *                   deixa de bater -- por isso o controlador le o corpo cru.
     */
    public static boolean confere(byte[] corpoBruto, String assinaturaRecebida, String segredo) {
        if (segredo == null || segredo.isBlank() || assinaturaRecebida == null || assinaturaRecebida.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = mac.doFinal(corpoBruto);

            // Alguns gateways prefixam o algoritmo ("sha256=..."). Aceitar os dois
            // formatos evita uma falha de integracao que so aparece em producao.
            String limpa = assinaturaRecebida.startsWith("sha256=")
                    ? assinaturaRecebida.substring(7)
                    : assinaturaRecebida;

            byte[] recebido;
            try {
                recebido = HexFormat.of().parseHex(limpa.toLowerCase());
            } catch (IllegalArgumentException naoEhHex) {
                recebido = java.util.Base64.getDecoder().decode(limpa);
            }
            return MessageDigest.isEqual(esperado, recebido);
        } catch (Exception e) {
            return false;
        }
    }
}
