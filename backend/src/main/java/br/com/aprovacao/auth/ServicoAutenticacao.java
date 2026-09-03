package br.com.aprovacao.auth;

import br.com.aprovacao.auth.DtosAutenticacao.LoginRequest;
import br.com.aprovacao.auth.DtosAutenticacao.SessaoResumo;
import br.com.aprovacao.auth.DtosAutenticacao.TokenResponse;
import br.com.aprovacao.auth.DtosAutenticacao.UsuarioResumo;
import br.com.aprovacao.common.ProblemaNegocio;
import br.com.aprovacao.config.PropriedadesPlataforma;
import br.com.aprovacao.conteudo.ServicoAuditoria;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 20 -- regras de sessao, e secao 21 -- bloqueio por tentativa de login.
 */
@Service
public class ServicoAutenticacao {

    private static final Logger log = LoggerFactory.getLogger(ServicoAutenticacao.class);

    /**
     * Hash descartavel de uma senha qualquer. Quando o e-mail nao existe ainda
     * gastamos um BCrypt, para que "conta inexistente" e "senha errada" levem o
     * mesmo tempo -- sem isso o tempo de resposta enumera a base de alunos.
     */
    private static final String HASH_FALSO =
            "$2a$12$C6UzMDM.H6dfI/f/IKcEe.SlpFmiy3PjPPZG9ExkoDPWiCFT3vLoO";

    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final TokenRecuperacaoRepository tokensRecuperacao;
    private final ServicoJwt jwt;
    private final PasswordEncoder encoder;
    private final PropriedadesPlataforma props;
    private final ServicoAuditoria auditoria;

    public ServicoAutenticacao(UsuarioRepository usuarios,
                               SessaoRepository sessoes,
                               TokenRecuperacaoRepository tokensRecuperacao,
                               ServicoJwt jwt,
                               PasswordEncoder encoder,
                               PropriedadesPlataforma props,
                               ServicoAuditoria auditoria) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.tokensRecuperacao = tokensRecuperacao;
        this.jwt = jwt;
        this.encoder = encoder;
        this.props = props;
        this.auditoria = auditoria;
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Transactional
    public TokenResponse login(LoginRequest req, UUID tenantId, String ip, String userAgent) {
        Optional<Usuario> encontrado = usuarios.buscarAtivoPorEmail(req.email(), tenantId);

        if (encontrado.isEmpty()) {
            encoder.matches(req.senha(), HASH_FALSO);
            throw credenciaisInvalidas();
        }

        Usuario usuario = encontrado.get();

        if (usuario.estaBloqueado()) {
            throw new ProblemaNegocio(HttpStatus.LOCKED, "conta-bloqueada",
                    "Conta temporariamente bloqueada por tentativas de login. Tente novamente em alguns minutos.");
        }

        if (!encoder.matches(req.senha(), usuario.getSenhaHash())) {
            boolean bloqueou = usuario.registrarFalhaDeLogin(
                    props.seguranca().maxFalhasLogin(), props.seguranca().bloqueioMinutos());
            usuarios.save(usuario);
            if (bloqueou) {
                log.warn("Conta {} bloqueada por excesso de falhas de login", usuario.getId());
                auditoria.registrar(usuario.getTenantId(), usuario.getId(), "LOGIN_BLOQUEADO",
                        "usuario", usuario.getId().toString(), ip, userAgent);
            }
            throw credenciaisInvalidas();
        }

        // Secao 20: 2FA obrigatorio para Admin e Suporte.
        if (usuario.exigeMfa(jwt.perfisComMfaObrigatorio())) {
            if (!usuario.isMfaAtivo()) {
                throw new ProblemaNegocio(HttpStatus.FORBIDDEN, "mfa-nao-configurado",
                        "Este perfil exige segundo fator. Configure o 2FA para continuar.");
            }
            if (req.codigoMfa() == null || req.codigoMfa().isBlank()) {
                throw new ProblemaNegocio(HttpStatus.UNAUTHORIZED, "mfa-obrigatorio",
                        "Informe o codigo do segundo fator.");
            }
            if (!ServicoTotp.codigoValido(usuario.getMfaSecret(), req.codigoMfa())) {
                throw new ProblemaNegocio(HttpStatus.UNAUTHORIZED, "mfa-invalido",
                        "Codigo do segundo fator invalido ou expirado.");
            }
        }

        usuario.registrarLoginBemSucedido();
        usuarios.save(usuario);

        aplicarLimiteDeDispositivos(usuario.getId());

        SessaoAberta aberta = abrirSessao(usuario, UUID.randomUUID(), req.dispositivo(), ip, userAgent);
        return montarResposta(usuario, aberta);
    }

    /**
     * Secao 10: "Limite de 2 dispositivos simultaneos; a terceira sessao derruba a
     * mais antiga." Aplicado no login, antes de abrir a nova sessao, para que o
     * limite conte a sessao que esta prestes a nascer.
     */
    private void aplicarLimiteDeDispositivos(UUID usuarioId) {
        List<Sessao> vivas = sessoes.listarVivas(usuarioId);
        int excedente = vivas.size() - (props.acesso().maxSessoesSimultaneas() - 1);
        for (int i = 0; i < excedente; i++) {
            vivas.get(i).revogar("LIMITE_DISPOSITIVOS");
        }
    }

    // -------------------------------------------------------------------------
    // Refresh com rotacao
    // -------------------------------------------------------------------------

    /**
     * Secao 20: "Rotaciona o refresh; reuso invalida a familia."
     *
     * <p>Um refresh ja rotacionado que reaparece so tem duas explicacoes: token
     * roubado, ou cliente com corrida de requisicoes. As duas terminam do mesmo
     * jeito -- derruba a familia e obriga login novo. Preferimos o falso positivo
     * ocasional a manter viva uma cadeia possivelmente comprometida.
     */
    @Transactional
    public TokenResponse rotacionar(String refreshToken, String ip, String userAgent) {
        String hash = jwt.hashDeRefresh(refreshToken);
        Sessao sessao = sessoes.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new ProblemaNegocio(HttpStatus.UNAUTHORIZED, "refresh-invalido",
                        "Sessao invalida. Faca login novamente."));

        if (!sessao.estaViva()) {
            int derrubadas = sessoes.revogarFamilia(sessao.getFamiliaId(), Instant.now(), "REUSO_DE_REFRESH");
            log.warn("Reuso de refresh detectado. Familia {} invalidada ({} sessoes).",
                    sessao.getFamiliaId(), derrubadas);
            auditoria.registrar(null, sessao.getUsuarioId(), "REUSO_REFRESH_DETECTADO",
                    "sessao", sessao.getId().toString(), ip, userAgent);
            throw new ProblemaNegocio(HttpStatus.UNAUTHORIZED, "refresh-reutilizado",
                    "Sessao encerrada por seguranca. Faca login novamente.");
        }

        Usuario usuario = usuarios.buscarAtivoPorId(sessao.getUsuarioId())
                .orElseThrow(() -> new ProblemaNegocio(HttpStatus.UNAUTHORIZED, "usuario-inativo",
                        "Conta indisponivel."));

        sessao.revogar("ROTACIONADO");
        SessaoAberta nova = abrirSessao(usuario, sessao.getFamiliaId(), sessao.getDispositivo(), ip, userAgent);
        return montarResposta(usuario, nova);
    }

    /** Secao 20: "Logout encerra a sessao no servidor -- nao basta apagar do navegador." */
    @Transactional
    public void logout(String refreshToken) {
        sessoes.findByRefreshTokenHash(jwt.hashDeRefresh(refreshToken))
                .ifPresent(s -> s.revogar("LOGOUT"));
    }

    @Transactional
    public void encerrarTodasAsSessoes(UUID usuarioId) {
        sessoes.revogarTodasDoUsuario(usuarioId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<SessaoResumo> listarSessoes(UUID usuarioId, UUID sessaoAtual) {
        return sessoes.listarVivas(usuarioId).stream()
                .map(s -> new SessaoResumo(s.getId(), s.getDispositivo(), s.getIp(),
                        s.getCriadoEm(), s.getExpiraEm(), s.getId().equals(sessaoAtual)))
                .toList();
    }

    @Transactional
    public void encerrarSessao(UUID usuarioId, UUID sessaoId) {
        Sessao sessao = sessoes.findById(sessaoId)
                .orElseThrow(() -> ProblemaNegocio.naoEncontrado("Sessao"));
        if (!sessao.getUsuarioId().equals(usuarioId)) {
            throw ProblemaNegocio.proibido("Esta sessao nao pertence a sua conta.");
        }
        sessao.revogar("ENCERRADA_PELO_TITULAR");
    }

    // -------------------------------------------------------------------------
    // Recuperacao de senha
    // -------------------------------------------------------------------------

    /**
     * Devolve o token em claro para quem for enviar o e-mail. O controlador nunca
     * o expoe na resposta HTTP -- a resposta e sempre 202, exista a conta ou nao,
     * para nao confirmar cadastro a um terceiro.
     */
    @Transactional
    public Optional<String> abrirRecuperacaoDeSenha(String email, UUID tenantId) {
        Optional<Usuario> usuario = usuarios.buscarAtivoPorEmail(email, tenantId);
        if (usuario.isEmpty()) {
            return Optional.empty();
        }
        tokensRecuperacao.invalidarPendentes(usuario.get().getId(), Instant.now());
        String token = jwt.gerarRefreshToken();
        tokensRecuperacao.save(new TokenRecuperacao(usuario.get().getId(), jwt.hashDeRefresh(token)));
        return Optional.of(token);
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        validarForcaDaSenha(novaSenha);
        TokenRecuperacao registro = tokensRecuperacao.findByTokenHash(jwt.hashDeRefresh(token))
                .filter(TokenRecuperacao::utilizavel)
                .orElseThrow(() -> new ProblemaNegocio(HttpStatus.GONE, "token-expirado",
                        "Link de recuperacao invalido ou expirado. Peca um novo."));

        Usuario usuario = usuarios.buscarAtivoPorId(registro.getUsuarioId())
                .orElseThrow(() -> ProblemaNegocio.naoEncontrado("Usuario"));

        usuario.setSenhaHash(encoder.encode(novaSenha));
        usuario.registrarLoginBemSucedido();
        usuarios.save(usuario);
        registro.marcarUsado();

        // Trocar a senha derruba tudo: se a conta foi comprometida, o atacante
        // perde os refresh tokens que ja tinha em maos.
        sessoes.revogarTodasDoUsuario(usuario.getId(), Instant.now());
    }

    /** Secao 21: BCrypt custo 12, minimo 10 caracteres, bloqueio de senhas vazadas. */
    public void validarForcaDaSenha(String senha) {
        if (senha == null || senha.length() < props.seguranca().senhaMinima()) {
            throw ProblemaNegocio.invalido("senha-fraca",
                    "A senha precisa de pelo menos " + props.seguranca().senhaMinima() + " caracteres.");
        }
        if (SenhasVazadas.contem(senha)) {
            throw ProblemaNegocio.invalido("senha-vazada",
                    "Esta senha aparece em vazamentos publicos conhecidos. Escolha outra.");
        }
    }

    // -------------------------------------------------------------------------

    /**
     * O refresh em claro so existe dentro deste par de chamadas: e devolvido junto
     * com a sessao, entregue na resposta e descartado. Persistido, so o hash.
     */
    private record SessaoAberta(Sessao sessao, String refreshEmClaro) {}

    private SessaoAberta abrirSessao(Usuario usuario, UUID familiaId, String dispositivo, String ip, String userAgent) {
        String refresh = jwt.gerarRefreshToken();
        Sessao sessao = new Sessao(usuario.getId(), familiaId, jwt.hashDeRefresh(refresh),
                dispositivo, userAgent, ip, jwt.expiracaoDoRefresh());
        sessoes.save(sessao);
        return new SessaoAberta(sessao, refresh);
    }

    private TokenResponse montarResposta(Usuario usuario, SessaoAberta aberta) {
        return TokenResponse.de(
                jwt.emitirAccessToken(usuario, aberta.sessao().getId()),
                aberta.refreshEmClaro(),
                jwt.accessTokenSegundos(),
                UsuarioResumo.de(usuario));
    }

    private ProblemaNegocio credenciaisInvalidas() {
        return new ProblemaNegocio(HttpStatus.UNAUTHORIZED, "credenciais-invalidas",
                "E-mail ou senha incorretos.");
    }
}
