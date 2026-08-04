package com.florez.backend.auth.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.florez.backend.TestcontainersConfig;
import com.florez.backend.auth.domain.model.Role;
import com.florez.backend.auth.domain.model.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfig.class)
class UserRepositoryAdapterIT {

    @Autowired
    private UserRepositoryAdapter userRepositoryAdapter;

    @Test
    void save_yFindByUsername_persistenYRecuperanElUsuario() {
        User user = User.createNew("adapter-user", "hashed-pwd", Role.ANALYST);

        User saved = userRepositoryAdapter.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<User> found = userRepositoryAdapter.findByUsername("adapter-user");
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.ANALYST);
    }

    @Test
    void existsByUsername_conUsernameNoExistente_devuelveFalse() {
        assertThat(userRepositoryAdapter.existsByUsername("no-such-user")).isFalse();
    }
}
