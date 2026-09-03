package br.com.aprovacao.auth;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Secao 18 -- tabela usuario. Secao 22 -- os campos aqui sao dado pessoal sob a
 * Lei 13.709/2018; a exclusao e logica (excluido_em) porque o registro fiscal do
 * pedido precisa sobreviver a exclusao do cadastro.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    private String cpf;
    private String whatsapp;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "email_verificado_em")
    private Instant emailVerificadoEm;

    @Column(name = "ultimo_acesso_em")
    private Instant ultimoAcessoEm;

    /** Secao 21 -- 5 falhas levam a 15 min de bloqueio por conta e por IP. */
    @Column(name = "falhas_login", nullable = false)
    private short falhasLogin;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "mfa_ativo", nullable = false)
    private boolean mfaAtivo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @Column(name = "excluido_em")
    private Instant excluidoEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_perfil", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "perfil_id")
    @Convert(converter = ConversorPerfil.class)
    private Set<Perfil> perfis = EnumSet.noneOf(Perfil.class);

    protected Usuario() {}

    public Usuario(UUID tenantId, String nome, String email, String senhaHash) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.nome = nome;
        this.email = email.toLowerCase();
        this.senhaHash = senhaHash;
        this.perfis = EnumSet.of(Perfil.ALUNO);
    }

    public boolean estaBloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(Instant.now());
    }

    /**
     * Conta a falha e devolve se o bloqueio foi acionado. Chamado tambem quando o
     * e-mail nao existe? Nao -- ali a resposta e a mesma, mas nao ha conta para
     * bloquear; quem segura a enumeracao de e-mails e o limite de 10/min da secao 19.
     */
    public boolean registrarFalhaDeLogin(int maxFalhas, int bloqueioMinutos) {
        falhasLogin++;
        atualizadoEm = Instant.now();
        if (falhasLogin >= maxFalhas) {
            bloqueadoAte = Instant.now().plusSeconds(bloqueioMinutos * 60L);
            falhasLogin = 0;
            return true;
        }
        return false;
    }

    public void registrarLoginBemSucedido() {
        falhasLogin = 0;
        bloqueadoAte = null;
        ultimoAcessoEm = Instant.now();
        atualizadoEm = Instant.now();
    }

    /**
     * Secao 22: "Exclusao anonimiza o cadastro mas preserva o registro fiscal do
     * pedido -- obrigacao legal se sobrepoe." O e-mail vira um valor irreversivel
     * para que o indice unico continue valido sem guardar o dado original.
     */
    public void anonimizar() {
        this.nome = "Titular removido";
        this.email = "removido+" + id + "@invalido.local";
        this.cpf = null;
        this.whatsapp = null;
        this.dataNascimento = null;
        this.mfaSecret = null;
        this.mfaAtivo = false;
        this.excluidoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public boolean exigeMfa(Set<String> perfisComMfa) {
        return perfis.stream().anyMatch(p -> perfisComMfa.contains(p.name()));
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public String getCpf() { return cpf; }
    public String getWhatsapp() { return whatsapp; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public Instant getEmailVerificadoEm() { return emailVerificadoEm; }
    public Instant getUltimoAcessoEm() { return ultimoAcessoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getExcluidoEm() { return excluidoEm; }
    public boolean isMfaAtivo() { return mfaAtivo; }
    public String getMfaSecret() { return mfaSecret; }
    public Set<Perfil> getPerfis() { return perfis; }

    public void setNome(String nome) { this.nome = nome; this.atualizadoEm = Instant.now(); }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public void setDataNascimento(LocalDate d) { this.dataNascimento = d; }
    public void setSenhaHash(String h) { this.senhaHash = h; this.atualizadoEm = Instant.now(); }
    public void setEmailVerificadoEm(Instant i) { this.emailVerificadoEm = i; }
    public void concederPerfil(Perfil p) { this.perfis.add(p); }
}
