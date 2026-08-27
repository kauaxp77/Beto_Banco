package com.betobanco.users.service;

import com.betobanco.config.PasswordEncoderConfig;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.Student;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.StudentRepository;
import com.betobanco.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserDirectoryService implements UserDirectory {

    private static final Logger log = LoggerFactory.getLogger(UserDirectoryService.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final StudentRepository students;
    private final PasswordEncoder encoder;

    public UserDirectoryService(UserRepository users, RoleRepository roles,
                                StudentRepository students, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.students = students;
        this.encoder = encoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> buscarAtivoPorId(UUID id) {
        return users.findById(id).filter(User::isActive).map(UserDirectoryService::paraConta);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> buscarPorEmail(String email) {
        return users.findByEmailIgnoreCase(normalizar(email)).map(UserDirectoryService::paraConta);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return users.existsByEmailIgnoreCase(normalizar(email));
    }

    @Override
    @Transactional
    public Optional<UserAccount> verificarCredenciais(String email, String senha) {
        Optional<User> encontrado = users.findByEmailIgnoreCase(normalizar(email));

        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        User usuario = encontrado.get();

        if (usuario.getPasswordHash() == null
                || !usuario.isActive()
                || !encoder.matches(senha, usuario.getPasswordHash())) {
            return Optional.empty();
        }

        promoverHashSeNecessario(usuario, senha);
        return Optional.of(paraConta(usuario));
    }

    @Override
    @Transactional
    public UserAccount registrar(String email, String senha, String nomeCompleto) {
        String normalizado = normalizar(email);
        if (users.existsByEmailIgnoreCase(normalizado)) {
            throw new BusinessException(ErrorCode.CONFLICT, "E-mail já cadastrado");
        }

        Role aluno = roles.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT ausente"));

        User usuario = new User(normalizado, encoder.encode(senha), nomeCompleto.trim());
        usuario.getRoles().add(aluno);
        users.saveAndFlush(usuario);

        // Cadastro publico e captura de lead: cria a conta, nao da acesso a
        // conteudo. Quem da acesso e o entitlement, concedido na Fase 3.
        students.saveAndFlush(new Student(usuario.getId()));

        return paraConta(usuario);
    }

    @Override
    @Transactional
    public UserAccount criarSemSenha(String email, String nomeCompleto) {
        String normalizado = normalizar(email);

        Optional<User> existente = users.findByEmailIgnoreCase(normalizado);
        if (existente.isPresent()) {
            return paraConta(existente.get());
        }

        Role aluno = roles.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT ausente"));

        User usuario = new User(normalizado, null, nomeCompleto.trim());
        usuario.getRoles().add(aluno);
        users.saveAndFlush(usuario);
        students.saveAndFlush(new Student(usuario.getId()));

        return paraConta(usuario);
    }

    @Override
    @Transactional
    public void redefinirSenha(UUID userId, String novaSenha) {
        User usuario = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        usuario.setPasswordHash(encoder.encode(novaSenha));
        users.saveAndFlush(usuario);
    }

    /**
     * Migracao silenciosa: no primeiro login bem-sucedido de um hash legado, a
     * senha e regravada no algoritmo atual. A base migra sozinha, um aluno por
     * vez, sem que ninguem precise redefinir nada.
     */
    private void promoverHashSeNecessario(User usuario, String senha) {
        if (!usuario.getPasswordHash().startsWith(PasswordEncoderConfig.PREFIXO_ATUAL)) {
            usuario.setPasswordHash(encoder.encode(senha));
            users.saveAndFlush(usuario);
            log.info("Hash de senha promovido para {} no usuario {}",
                    PasswordEncoderConfig.ID_ATUAL, usuario.getId());
        }
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static UserAccount paraConta(User usuario) {
        return new UserAccount(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getFullName(),
                usuario.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }
}
