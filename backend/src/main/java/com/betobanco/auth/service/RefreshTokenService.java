package com.betobanco.auth.service;

import com.betobanco.auth.entity.RefreshToken;
import com.betobanco.auth.repository.RefreshTokenRepository;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int BYTES = 32; // 256 bits

    private final RefreshTokenRepository repo;
    private final UserDirectory users;
    private final Duration validade;
    private final int maxSessoes;
    private final int alertaIpsDistintos;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo,
                               UserDirectory users,
                               @Value("${betobanco.auth.refresh-token-days}") long dias,
                               @Value("${betobanco.auth.max-active-sessions:2}") int maxSessoes,
                               @Value("${betobanco.auth.alert-distinct-ips-24h:4}") int alertaIpsDistintos) {
        this.repo = repo;
        this.users = users;
        this.validade = Duration.ofDays(dias);
        this.maxSessoes = maxSessoes;
        this.alertaIpsDistintos = alertaIpsDistintos;
    }

    /** De onde a sessao nasceu. Secao 10 -- protecao de conteudo. */
    public record Origem(String ip, String userAgent) {
        public static final Origem DESCONHECIDA = new Origem(null, null);
    }

    public record Rotacao(UserAccount usuario, String novoValor) {
    }

    @Transactional
    public String emitir(UserAccount usuario) {
        return emitir(usuario, Origem.DESCONHECIDA);
    }

    /**
     * Emite um refresh token e aplica a protecao de conteudo da secao 10.
     *
     * <p>"Limite de 2 dispositivos simultaneos; a terceira sessao derruba a mais
     * antiga." O corte acontece ANTES de gravar a nova, e o excedente e
     * calculado contando a sessao que esta prestes a nascer -- senao o limite
     * efetivo seria de tres, nao de dois.
     *
     * <p>A escolha de derrubar em vez de recusar e deliberada: recusar o login
     * do aluno que trocou de celular seria puni-lo por um limite que existe para
     * conter compartilhamento de conta, nao uso legitimo.
     */
    @Transactional
    public String emitir(UserAccount usuario, Origem origem) {
        aplicarLimiteDeDispositivos(usuario.id());
        alertarSeAcessoAnomalo(usuario.id());

        String valor = gerarValor();
        repo.saveAndFlush(new RefreshToken(
                usuario.id(), hash(valor), Instant.now().plus(validade),
                origem.ip(), origem.userAgent()));
        return valor;
    }

    private void aplicarLimiteDeDispositivos(UUID userId) {
        List<RefreshToken> vigentes = repo.vigentesDe(userId);
        int excedente = vigentes.size() - (maxSessoes - 1);

        for (int i = 0; i < excedente; i++) {
            RefreshToken maisAntiga = vigentes.get(i);
            maisAntiga.revogar();
            repo.save(maisAntiga);
            log.info("Sessao mais antiga de {} derrubada pelo limite de {} dispositivos.",
                    userId, maxSessoes);
        }
    }

    /**
     * Secao 10: "Alerta automatico quando uma conta e acessada de mais de 4 IPs
     * distintos em 24h." Alerta, e nao bloqueio: a mesma contagem sobe quando um
     * aluno viaja ou troca de operadora, e bloquear seria trancar o cliente
     * legitimo do lado de fora por um sinal que e apenas indicio.
     */
    private void alertarSeAcessoAnomalo(UUID userId) {
        long ips = repo.contarIpsDistintosDesde(userId, Instant.now().minus(Duration.ofHours(24)));
        if (ips > alertaIpsDistintos) {
            log.warn("Conta {} acessada de {} IPs distintos em 24h (limite de alerta: {}). "
                            + "Possivel compartilhamento de conta.",
                    userId, ips, alertaIpsDistintos);
        }
    }

    /**
     * Troca um refresh token por outro. Devolve vazio quando o token e
     * desconhecido, expirado, revogado, do usuario bloqueado — ou quando ja
     * foi rotacionado antes, caso em que a cadeia inteira e derrubada.
     */
    @Transactional
    public Optional<Rotacao> rotacionar(String valorEmClaro) {
        if (valorEmClaro == null || valorEmClaro.isBlank()) {
            return Optional.empty();
        }

        Optional<RefreshToken> encontrado = repo.findByTokenHash(hash(valorEmClaro));
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken atual = encontrado.get();

        // Reuso: este token ja gerou um sucessor. Como o valor em claro so
        // deveria existir no cliente legitimo, sua reaparicao significa copia.
        // Derruba tudo do usuario, inclusive a sessao que ainda funcionava.
        if (atual.foiRotacionado()) {
            log.warn("Reuso de refresh token detectado para o usuario {}; revogando a cadeia",
                    atual.getUserId());
            repo.revogarVigentesDe(atual.getUserId(), Instant.now());
            return Optional.empty();
        }

        if (!atual.estaVigente()) {
            return Optional.empty();
        }

        Optional<UserAccount> dono = users.buscarAtivoPorId(atual.getUserId());
        if (dono.isEmpty()) {
            return Optional.empty();
        }

        String novoValor = gerarValor();
        RefreshToken sucessor = repo.saveAndFlush(new RefreshToken(
                atual.getUserId(), hash(novoValor), Instant.now().plus(validade)));

        atual.revogar();
        atual.setReplacedBy(sucessor.getId());
        repo.saveAndFlush(atual);

        return Optional.of(new Rotacao(dono.get(), novoValor));
    }

    @Transactional
    public void revogar(String valorEmClaro) {
        if (valorEmClaro == null || valorEmClaro.isBlank()) {
            return;
        }
        repo.findByTokenHash(hash(valorEmClaro)).ifPresent(t -> {
            t.revogar();
            repo.saveAndFlush(t);
        });
    }

    @Transactional
    public void revogarTodosDe(UUID userId) {
        repo.revogarVigentesDe(userId, Instant.now());
    }

    private String gerarValor() {
        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 basta aqui: o valor tem 256 bits de entropia vinda de
     * SecureRandom, entao nao ha o que quebrar por forca bruta ou dicionario —
     * ao contrario de uma senha escolhida por humano, que exige Argon2.
     */
    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
