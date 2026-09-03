package br.com.aprovacao.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secao 21: "BCrypt custo 12, minimo 10 caracteres, bloqueio de senhas vazadas."
 *
 * <p>Lista local em vez de consulta ao Have I Been Pwned: a checagem acontece no
 * cadastro e na troca de senha, e um provedor externo indisponivel nao pode
 * bloquear nenhum dos dois. A lista fica em recurso de classpath e pode crescer
 * sem tocar em codigo.
 */
final class SenhasVazadas {

    private static final Logger log = LoggerFactory.getLogger(SenhasVazadas.class);
    private static final String RECURSO = "/seguranca/senhas-vazadas.txt";
    private static final Set<String> LISTA = carregar();

    private SenhasVazadas() {}

    static boolean contem(String senha) {
        return senha != null && LISTA.contains(senha.toLowerCase(Locale.ROOT));
    }

    private static Set<String> carregar() {
        Set<String> lista = new HashSet<>();
        try (InputStream in = SenhasVazadas.class.getResourceAsStream(RECURSO)) {
            if (in == null) {
                log.warn("Lista de senhas vazadas ausente em {}. A validacao segue apenas pelo tamanho minimo.", RECURSO);
                return Set.of();
            }
            try (BufferedReader leitor = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = leitor.readLine()) != null) {
                    String limpa = linha.trim().toLowerCase(Locale.ROOT);
                    if (!limpa.isEmpty() && !limpa.startsWith("#")) {
                        lista.add(limpa);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Falha ao ler a lista de senhas vazadas", e);
            return Set.of();
        }
        return Set.copyOf(lista);
    }
}
