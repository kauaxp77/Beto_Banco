package com.betobanco.auth;

import com.betobanco.auth.repository.RefreshTokenRepository;
import com.betobanco.auth.service.RefreshTokenService;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenServiceTest extends PostgresTestBase {

    @Autowired
    private RefreshTokenService tokens;

    @Autowired
    private RefreshTokenRepository repo;

    @Autowired
    private UserRepository users;

    private UserAccount novoUsuario(String email) {
        User u = users.saveAndFlush(new User(email, "{bcrypt}xxx", "Fulano"));
        return new UserAccount(u.getId(), u.getEmail(), u.getFullName(), java.util.Set.of());
    }

    @Test
    void oValorEmClaroNaoEhGuardadoNoBanco() {
        UserAccount u = novoUsuario("claro@exemplo.com");

        String valor = tokens.emitir(u);

        assertThat(valor).isNotBlank();
        assertThat(repo.findByTokenHash(valor)).isEmpty();
    }

    @Test
    void rotacionarDevolveNovoValorEInvalidaOAnterior() {
        UserAccount u = novoUsuario("rotaciona@exemplo.com");
        String primeiro = tokens.emitir(u);

        RefreshTokenService.Rotacao r = tokens.rotacionar(primeiro).orElseThrow();

        assertThat(r.usuario().id()).isEqualTo(u.id());
        assertThat(r.novoValor()).isNotEqualTo(primeiro);
        assertThat(tokens.rotacionar(primeiro)).isEmpty();
    }

    @Test
    void reusarUmTokenJaRotacionadoDerrubaACadeiaInteira() {
        UserAccount u = novoUsuario("roubado@exemplo.com");
        String t1 = tokens.emitir(u);
        String t2 = tokens.rotacionar(t1).orElseThrow().novoValor();
        String t3 = tokens.rotacionar(t2).orElseThrow().novoValor();

        // t1 reaparece: alguem tem uma copia.
        assertThat(tokens.rotacionar(t1)).isEmpty();

        // A cadeia inteira cai junto, inclusive o token que ainda era valido.
        assertThat(tokens.rotacionar(t3)).isEmpty();
    }

    @Test
    void tokenDesconhecidoEhRecusadoSemExplodir() {
        assertThat(tokens.rotacionar("valor-que-nunca-existiu")).isEmpty();
        assertThat(tokens.rotacionar("")).isEmpty();
        assertThat(tokens.rotacionar(null)).isEmpty();
    }

    @Test
    void tokenDeUsuarioBloqueadoEhRecusado() {
        UserAccount u = novoUsuario("bloqueado@exemplo.com");
        String valor = tokens.emitir(u);
        User entidade = users.findById(u.id()).orElseThrow();
        entidade.setStatus(User.BLOCKED);
        users.saveAndFlush(entidade);

        assertThat(tokens.rotacionar(valor)).isEmpty();
    }

    @Test
    void revogarTodosEncerraAsSessoesDoUsuario() {
        UserAccount u = novoUsuario("sai@exemplo.com");
        String a = tokens.emitir(u);
        String b = tokens.emitir(u);

        tokens.revogarTodosDe(u.id());

        assertThat(tokens.rotacionar(a)).isEmpty();
        assertThat(tokens.rotacionar(b)).isEmpty();
    }

    @Test
    void revogarUmNaoAfetaOutraSessaoDoMesmoUsuario() {
        UserAccount u = novoUsuario("duasabas@exemplo.com");
        String aba1 = tokens.emitir(u);
        String aba2 = tokens.emitir(u);

        tokens.revogar(aba1);

        assertThat(tokens.rotacionar(aba1)).isEmpty();
        assertThat(tokens.rotacionar(aba2)).isPresent();
    }

    @Test
    void doisUsuariosNaoInterferemEntreSi() {
        UserAccount a = novoUsuario("a@exemplo.com");
        UserAccount b = novoUsuario("b@exemplo.com");
        String tokenA = tokens.emitir(a);
        String tokenB = tokens.emitir(b);

        tokens.revogarTodosDe(a.id());

        assertThat(tokens.rotacionar(tokenA)).isEmpty();
        Optional<RefreshTokenService.Rotacao> rb = tokens.rotacionar(tokenB);
        assertThat(rb).isPresent();
        assertThat(rb.orElseThrow().usuario().id()).isEqualTo(b.id());
    }
}
