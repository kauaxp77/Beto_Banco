package br.com.aprovacao.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Secao 18, regra de modelagem: dinheiro em centavos, inteiro, nunca float.
 *
 * <p>Todo calculo comercial passa por aqui para que o arredondamento aconteca em
 * um lugar so -- desconto de cupom em percentual e a operacao onde um centavo
 * perdido vira divergencia na reconciliacao diaria (secao 12).
 */
public final class Dinheiro {

    private static final Locale BR = Locale.of("pt", "BR");

    private Dinheiro() {}

    /** Desconto percentual com arredondamento meio-para-cima. */
    public static long aplicarPercentual(long centavos, int percentual) {
        if (percentual <= 0) {
            return 0L;
        }
        return BigDecimal.valueOf(centavos)
                .multiply(BigDecimal.valueOf(percentual))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public static String formatar(long centavos) {
        return NumberFormat.getCurrencyInstance(BR)
                .format(BigDecimal.valueOf(centavos).movePointLeft(2));
    }
}
