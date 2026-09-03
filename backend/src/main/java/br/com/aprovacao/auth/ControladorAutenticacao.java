package br.com.aprovacao.auth;

import br.com.aprovacao.auth.DtosAutenticacao.LoginRequest;
import br.com.aprovacao.auth.DtosAutenticacao.RecuperarSenhaRequest;
import br.com.aprovacao.auth.DtosAutenticacao.RedefinirSenhaRequest;
import br.com.aprovacao.auth.DtosAutenticacao.RefreshRequest;
import br.com.aprovacao.auth.DtosAutenticacao.SessaoResumo;
import br.com.aprovacao.auth.DtosAutenticacao.TokenResponse;
import br.com.aprovacao.config.FiltroTenant;
import br.com.aprovacao.config.PropriedadesPlataforma;
import br.com.aprovacao.config.UsuarioAutenticado;
import br.com.aprovacao.lgpd.ServicoEmail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Secao 19 -- rotas /auth. Todas publicas exceto a gestao de sessoes. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacao", description = "Login, rotacao de refresh e recuperacao de senha (secoes 19 e 20)")
public class ControladorAutenticacao {

    private final ServicoAutenticacao servico;
    private final ServicoEmail email;
    private final PropriedadesPlataforma props;

    public ControladorAutenticacao(ServicoAutenticacao servico, ServicoEmail email, PropriedadesPlataforma props) {
        this.servico = servico;
        this.email = email;
        this.props = props;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Devolve access (15 min) + refresh (30 d)")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return servico.login(req, FiltroTenant.atual(props.tenantPadrao()), ip(http), http.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotaciona o refresh; reuso invalida a familia")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        return servico.rotacionar(req.refreshToken(), ip(http), http.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Encerra a sessao no servidor")
    public void logout(@Valid @RequestBody RefreshRequest req) {
        servico.logout(req.refreshToken());
    }

    /**
     * Secao 19 -- token de uso unico, 30 min.
     *
     * <p>Responde 202 exista a conta ou nao: uma resposta diferente para e-mail
     * inexistente transformaria esta rota em verificador de cadastro.
     */
    @PostMapping("/senha/recuperar")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envia link de recuperacao; responde 202 mesmo se o e-mail nao existir")
    public void recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest req) {
        servico.abrirRecuperacaoDeSenha(req.email(), FiltroTenant.atual(props.tenantPadrao()))
                .ifPresent(token -> email.enviarRecuperacaoDeSenha(req.email(), token));
    }

    @PostMapping("/senha/redefinir")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefine a senha e derruba todas as sessoes da conta")
    public void redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest req) {
        servico.redefinirSenha(req.token(), req.novaSenha());
    }

    /** Secao 20: "Aluno ve e derruba as proprias sessoes ativas no perfil." */
    @GetMapping("/sessoes")
    @Operation(summary = "Lista as sessoes ativas da conta")
    public List<SessaoResumo> sessoes() {
        UsuarioAutenticado u = UsuarioAutenticado.obrigatorio();
        return servico.listarSessoes(u.id(), u.sessaoId());
    }

    @DeleteMapping("/sessoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Encerra uma sessao especifica da propria conta")
    public void encerrarSessao(@PathVariable UUID id) {
        servico.encerrarSessao(UsuarioAutenticado.obrigatorio().id(), id);
    }

    @DeleteMapping("/sessoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Encerra todas as sessoes da conta")
    public ResponseEntity<Void> encerrarTodas() {
        servico.encerrarTodasAsSessoes(UsuarioAutenticado.obrigatorio().id());
        return ResponseEntity.noContent().build();
    }

    private String ip(HttpServletRequest req) {
        String encaminhado = req.getHeader("X-Forwarded-For");
        return (encaminhado != null && !encaminhado.isBlank())
                ? encaminhado.split(",")[0].trim()
                : req.getRemoteAddr();
    }
}
