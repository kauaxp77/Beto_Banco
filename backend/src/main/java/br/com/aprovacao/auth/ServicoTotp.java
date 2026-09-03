package br.com.aprovacao.auth;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TOTP (RFC 6238) para o 2FA obrigatorio de Admin e Suporte (secao 20).
 *
 * <p>Implementacao direta em vez de biblioteca: o algoritmo tem trinta linhas e
 * uma dependencia a menos e uma superficie a menos no dependency-check da secao 21.
 * Aceita a janela anterior e a seguinte para tolerar relogio dessincronizado.
 */
public final class ServicoTotp {

    private static final int DIGITOS = 6;
    private static final int PASSO_SEGUNDOS = 30;
    private static final int JANELAS_TOLERADAS = 1;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private ServicoTotp() {}

    public static String gerarSegredo() {
        byte[] bytes = new byte[20];
        ALEATORIO.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static boolean codigoValido(String segredoBase64, String codigoInformado) {
        if (segredoBase64 == null || codigoInformado == null) {
            return false;
        }
        String limpo = codigoInformado.replaceAll("\\D", "");
        if (limpo.length() != DIGITOS) {
            return false;
        }
        byte[] segredo = Base64.getDecoder().decode(segredoBase64);
        long contador = Instant.now().getEpochSecond() / PASSO_SEGUNDOS;

        for (int desvio = -JANELAS_TOLERADAS; desvio <= JANELAS_TOLERADAS; desvio++) {
            if (constanteIguais(gerarCodigo(segredo, contador + desvio), limpo)) {
                return true;
            }
        }
        return false;
    }

    private static String gerarCodigo(byte[] segredo, long contador) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(segredo, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(contador).array());

            int deslocamento = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[deslocamento] & 0x7F) << 24)
                    | ((hash[deslocamento + 1] & 0xFF) << 16)
                    | ((hash[deslocamento + 2] & 0xFF) << 8)
                    | (hash[deslocamento + 3] & 0xFF);

            return String.format("%0" + DIGITOS + "d", binario % (int) Math.pow(10, DIGITOS));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular TOTP", e);
        }
    }

    /** Comparacao em tempo constante: evita descobrir o codigo por medicao de tempo. */
    private static boolean constanteIguais(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diferenca = 0;
        for (int i = 0; i < a.length(); i++) {
            diferenca |= a.charAt(i) ^ b.charAt(i);
        }
        return diferenca == 0;
    }
}
