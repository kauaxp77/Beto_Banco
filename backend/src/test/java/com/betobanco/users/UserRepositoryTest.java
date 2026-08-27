package com.betobanco.users;

import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UserRepositoryTest extends PostgresTestBase {

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Test
    void persisteEBuscaPorEmailIgnorandoCaixa() {
        Role aluno = roles.findByName("ROLE_STUDENT").orElseThrow();
        User novo = new User("Maria@Exemplo.com", "{bcrypt}xxx", "Maria");
        novo.getRoles().add(aluno);
        users.saveAndFlush(novo);

        assertThat(users.findByEmailIgnoreCase("MARIA@exemplo.COM")).isPresent();
        assertThat(users.existsByEmailIgnoreCase("maria@exemplo.com")).isTrue();
    }

    @Test
    void idEhGeradoEStatusPadraoEhAtivo() {
        User novo = users.saveAndFlush(new User("joao@exemplo.com", null, "Joao"));

        assertThat(novo.getId()).isNotNull();
        assertThat(novo.getStatus()).isEqualTo("ACTIVE");
        assertThat(novo.isActive()).isTrue();
    }

    @Test
    void asRolesSaoCarregadas() {
        Role admin = roles.findByName("ROLE_ADMIN").orElseThrow();
        User novo = new User("chefe@exemplo.com", "{bcrypt}xxx", "Chefe");
        novo.getRoles().add(admin);
        users.saveAndFlush(novo);

        User lido = users.findByEmailIgnoreCase("chefe@exemplo.com").orElseThrow();
        assertThat(lido.getRoles()).extracting(Role::getName).containsExactly("ROLE_ADMIN");
    }

    @Test
    void asTresRolesObrigatoriasExistem() {
        assertThat(roles.findByName("ROLE_STUDENT")).isPresent();
        assertThat(roles.findByName("ROLE_ADMIN")).isPresent();
        assertThat(roles.findByName("ROLE_INSTRUCTOR")).isPresent();
    }
}
