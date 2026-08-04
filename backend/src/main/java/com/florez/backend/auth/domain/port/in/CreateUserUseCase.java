package com.florez.backend.auth.domain.port.in;

import com.florez.backend.auth.domain.model.Role;
import com.florez.backend.auth.domain.model.User;

public interface CreateUserUseCase {

    User createUser(String username, String rawPassword, Role role);
}
