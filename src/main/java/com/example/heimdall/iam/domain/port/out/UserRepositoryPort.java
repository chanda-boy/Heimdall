package com.example.heimdall.iam.domain.port.out;

import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.model.UserId;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findByEmail(Email email);
    Optional<User> findById(UserId id);
    User save(User user);
    boolean existsByEmail(Email email);
}
