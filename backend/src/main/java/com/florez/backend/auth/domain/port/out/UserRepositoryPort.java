package com.florez.backend.auth.domain.port.out;

import com.florez.backend.auth.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    long count();
}
