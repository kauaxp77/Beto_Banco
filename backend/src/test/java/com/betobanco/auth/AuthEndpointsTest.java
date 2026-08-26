package com.betobanco.auth;

import com.betobanco.auth.entity.TokenPurpose;
import com.betobanco.auth.service.PasswordResetService;
import com.betobanco.config.PasswordEncoderConfig;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.StudentRepository;
import com.betobanco.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthEndpointsTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private StudentRepository students;

    @Autowired
    private PasswordResetService resets;

    private User criarLegado(String email, String senha, String role) {
        Role r = roles.findByName(role).orElseThrow();
        User u = new User(email, "{bcrypt}" + new BCryptPasswordEncoder().encode(senha), "Fulano");
        u.getRoles().add(r);
        users.saveAndFlush(u);
        students.saveAndFlush(new com.betobanco.users.entity.Student(u.getId()));
        return u;
    }

    private String login(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + senha + "\"}";
    }

    private String registro(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\","
                + "\"fullName\":\"Novo Aluno\"}";
    }

    private String refreshJson(String valor) {
        return "{\"refreshToken\":\"" + valor + "\"}";
    }

    // ---------- login ----------

    @Test
    void loginValidoDevolveAccessTokenERefreshToken() throws Exception {
        criarLegado("ok@exemplo.com", "senha123", "ROLE_STUDENT");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ok@exemplo.com", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void oHashLegadoEhPromovidoParaArgon2NoPrimeiroLogin() throws Exception {
        User u = criarLegado("legado@exemplo.com", "senha123", "ROLE_STUDENT");
        assertThat(u.getPasswordHash()).startsWith("{bcrypt}");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("legado@exemplo.com", "senha123"))).andExpect(status().isOk());

        User depois = users.findByEmailIgnoreCase("legado@exemplo.com").orElseThrow();
        assertThat(depois.getPasswordHash()).startsWith(PasswordEncoderConfig.PREFIXO_ATUAL);

        // E a senha continua funcionando depois da promocao.
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("legado@exemplo.com", "senha123"))).andExpect(status().isOk());
    }

    @Test
    void senhaErradaEEmailInexistenteDaoAMesmaResposta() throws Exception {
        criarLegado("errada@exemplo.com", "certa", "ROLE_STUDENT");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("errada@exemplo.com", "outra")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Credenciais inválidas"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ninguem@exemplo.com", "qualquer")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Credenciais inválidas"));
    }

    @Test
    void usuarioBloqueadoNaoEntra() throws Exception {
        User u = criarLegado("bloqueado@exemplo.com", "senha123", "ROLE_STUDENT");
        u.setStatus(User.BLOCKED);
        users.saveAndFlush(u);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("bloqueado@exemplo.com", "senha123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailComCaixaDiferenteEntra() throws Exception {
        criarLegado("caixa@exemplo.com", "senha123", "ROLE_STUDENT");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("CAIXA@Exemplo.COM", "senha123"))).andExpect(status().isOk());
    }

    @Test
    void corpoInvalidoDevolve422() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void meDevolveOUsuarioDoTokenSemVazarOHash() throws Exception {
        criarLegado("eu@exemplo.com", "senha123", "ROLE_STUDENT");
        String token = tokenDe("eu@exemplo.com", "senha123");

        String corpo = mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("eu@exemplo.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_STUDENT"))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("passwordHash", "bcrypt", "argon2");
    }

    // ---------- refresh e logout ----------

    @Test
    void refreshValidoDevolveNovoParEInvalidaOAnterior() throws Exception {
        criarLegado("refresh@exemplo.com", "senha123", "ROLE_STUDENT");
        String primeiro = refreshDe("refresh@exemplo.com", "senha123");

        String corpo = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(primeiro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(corpo, "$.data.refreshToken")).isNotEqualTo(primeiro);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(primeiro)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reusarUmRefreshJaRotacionadoDerrubaTodasAsSessoes() throws Exception {
        criarLegado("roubo@exemplo.com", "senha123", "ROLE_STUDENT");
        String t1 = refreshDe("roubo@exemplo.com", "senha123");
        String t2 = JsonPath.read(mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(t1)))
                .andReturn().getResponse().getContentAsString(), "$.data.refreshToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(t1)))
                .andExpect(status().isUnauthorized());

        // t2 era legitimo e cai junto: e o preco de conter um roubo.
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(t2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEncerraApenasASessaoInformadaESempreDevolve204() throws Exception {
        criarLegado("logout@exemplo.com", "senha123", "ROLE_STUDENT");
        String aba1 = refreshDe("logout@exemplo.com", "senha123");
        String aba2 = refreshDe("logout@exemplo.com", "senha123");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(aba1)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(aba1)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(aba2)))
                .andExpect(status().isOk());

        // Token desconhecido tambem devolve 204: 404 revelaria quais existem.
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson("qualquer")))
                .andExpect(status().isNoContent());
    }

    // ---------- registro e definicao de senha ----------

    @Test
    void registroCriaAlunoEEmailRepetidoDevolve409() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("novo@exemplo.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("novo@exemplo.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_STUDENT"));

        User criado = users.findByEmailIgnoreCase("novo@exemplo.com").orElseThrow();
        assertThat(students.findById(criado.getId())).isPresent();

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("NOVO@Exemplo.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void senhaCurtaDevolve422() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"curta@exemplo.com\",\"password\":\"123\","
                                + "\"fullName\":\"Alguem\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("password"));
    }

    @Test
    void forgotPasswordRespondeIgualParaEmailQueExisteOuNao() throws Exception {
        criarLegado("existe@exemplo.com", "senha123", "ROLE_STUDENT");

        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"existe@exemplo.com\"}")).andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nao-existe@exemplo.com\"}")).andExpect(status().isNoContent());
    }

    @Test
    void tokenDePrimeiroAcessoDefineASenhaEPermiteLogin() throws Exception {
        Role r = roles.findByName("ROLE_STUDENT").orElseThrow();
        User u = new User("primeiro@exemplo.com", null, "Primeiro Acesso");
        u.getRoles().add(r);
        users.saveAndFlush(u);

        String token = resets.criarToken(
                new UserAccount(u.getId(), u.getEmail(), u.getFullName(), java.util.Set.of()),
                TokenPurpose.FIRST_ACCESS);

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"minha-senha-123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("primeiro@exemplo.com", "minha-senha-123")))
                .andExpect(status().isOk());
    }

    @Test
    void tokenDeDefinicaoDeSenhaNaoServeDuasVezes() throws Exception {
        User u = criarLegado("umavez@exemplo.com", "senha123", "ROLE_STUDENT");
        String token = resets.criarToken(
                new UserAccount(u.getId(), u.getEmail(), u.getFullName(), java.util.Set.of()),
                TokenPurpose.RESET);

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"primeira-vez-123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"segunda-vez-123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ERROR"));
    }

    @Test
    void redefinirSenhaEncerraAsSessoesAbertas() throws Exception {
        User u = criarLegado("sessoes@exemplo.com", "senha123", "ROLE_STUDENT");
        String refresh = refreshDe("sessoes@exemplo.com", "senha123");

        String token = resets.criarToken(
                new UserAccount(u.getId(), u.getEmail(), u.getFullName(), java.util.Set.of()),
                TokenPurpose.RESET);

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"nova-senha-123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshJson(refresh)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- perfil do aluno ----------

    @Test
    void studentsMeDevolveEAtualizaOProprioPerfil() throws Exception {
        criarLegado("perfil@exemplo.com", "senha123", "ROLE_STUDENT");
        String token = tokenDe("perfil@exemplo.com", "senha123");

        mockMvc.perform(get("/students/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("perfil@exemplo.com"));

        mockMvc.perform(put("/students/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Nome Novo\",\"phone\":\"11999998888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Nome Novo"))
                .andExpect(jsonPath("$.data.phone").value("11999998888"));
    }

    @Test
    void studentsMeSemTokenDevolve401() throws Exception {
        mockMvc.perform(get("/students/me")).andExpect(status().isUnauthorized());
    }

    // ---------- auxiliares ----------

    private String tokenDe(String email, String senha) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(login(email, senha)))
                .andReturn().getResponse().getContentAsString(), "$.data.accessToken");
    }

    private String refreshDe(String email, String senha) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(login(email, senha)))
                .andReturn().getResponse().getContentAsString(), "$.data.refreshToken");
    }
}
