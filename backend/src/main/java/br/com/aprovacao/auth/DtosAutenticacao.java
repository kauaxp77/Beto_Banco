package br.com.aprovacao.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Payloads de /api/v1/auth. Secao 19: JSON em snake_case, datas ISO 8601 com fuso. */
public final class DtosAutenticacao {

    private DtosAutenticacao() {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String senha,
            String dispositivo,
            /** Codigo TOTP. Obrigatorio para Admin e Suporte (secao 20). */
            String codigoMfa) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UsuarioResumo usuario) {

        public static TokenResponse de(String access, String refresh, long expiraEm, UsuarioResumo usuario) {
            return new TokenResponse(access, refresh, "Bearer", expiraEm, usuario);
        }
    }

    public record UsuarioResumo(UUID id, String nome, String email, List<String> perfis) {
        public static UsuarioResumo de(Usuario u) {
            return new UsuarioResumo(u.getId(), u.getNome(), u.getEmail(),
                    u.getPerfis().stream().map(Enum::name).sorted().toList());
        }
    }

    public record CadastroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email String email,
            @NotBlank String senha,
            String whatsapp,
            /** Secao 22: aceite registrado com data, hora, IP e versao do texto. */
            @NotBlank String versaoTermos,
            boolean consenteMarketingWhatsapp) {}

    public record RecuperarSenhaRequest(@NotBlank @Email String email) {}

    public record RedefinirSenhaRequest(@NotBlank String token, @NotBlank String novaSenha) {}

    /** Secao 20: "Aluno ve e derruba as proprias sessoes ativas no perfil." */
    public record SessaoResumo(
            UUID id, String dispositivo, String ip, Instant criadoEm, Instant expiraEm, boolean atual) {}
}
