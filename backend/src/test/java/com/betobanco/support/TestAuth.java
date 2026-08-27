package com.betobanco.support;

import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cria (se preciso) e loga um usuario com a role pedida, devolvendo o access token. */
public final class TestAuth {

    private TestAuth() {
    }

    public static String logarComo(MockMvc mockMvc, UserRepository users, RoleRepository roles,
                                   String email, String role) throws Exception {
        if (users.findByEmailIgnoreCase(email).isEmpty()) {
            Role r = roles.findByName(role).orElseThrow();
            User u = new User(email,
                    "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"), "Usuario Teste");
            u.getRoles().add(r);
            users.saveAndFlush(u);
        }
        String json = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.accessToken");
    }
}
