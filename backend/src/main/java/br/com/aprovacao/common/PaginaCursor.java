package br.com.aprovacao.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/**
 * Secao 19: paginacao por cursor, com next_cursor na resposta.
 *
 * <p>Offset degrada em catalogo grande e duplica item quando o dado muda entre
 * paginas; cursor opaco evita os dois e nao expoe o formato ao cliente.
 */
public record PaginaCursor<T>(List<T> itens, String nextCursor) {

    public static final int LIMITE_PADRAO = 20;
    public static final int LIMITE_MAXIMO = 100;

    public static int normalizarLimite(Integer limit) {
        if (limit == null || limit <= 0) {
            return LIMITE_PADRAO;
        }
        return Math.min(limit, LIMITE_MAXIMO);
    }

    /**
     * Monta a pagina a partir de uma lista consultada com limite + 1 linhas: a
     * linha excedente so responde "existe proxima pagina" e nao e devolvida.
     */
    public static <T> PaginaCursor<T> de(List<T> comSobra, int limite, Function<T, String> chave) {
        if (comSobra.size() <= limite) {
            return new PaginaCursor<>(comSobra, null);
        }
        List<T> pagina = comSobra.subList(0, limite);
        return new PaginaCursor<>(pagina, codificar(chave.apply(pagina.get(limite - 1))));
    }

    public static String codificar(String valor) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodificar(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw ProblemaNegocio.invalido("cursor-invalido", "O cursor enviado nao e valido.");
        }
    }
}
