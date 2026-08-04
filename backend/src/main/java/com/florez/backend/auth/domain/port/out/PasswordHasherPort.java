package com.florez.backend.auth.domain.port.out;

public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
