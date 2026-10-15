package com.example.heimdall.iam.infrastructure.persistence;

import com.example.heimdall.iam.domain.exception.UserAlreadyExistsException;
import com.example.heimdall.iam.domain.model.*;
import com.example.heimdall.iam.domain.port.out.UserRepositoryPort;
import com.example.heimdall.iam.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.example.heimdall.iam.infrastructure.persistence.repository.SpringDataUserRepository;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresUserRepositoryAdapter implements UserRepositoryPort {
    private final SpringDataUserRepository repository;
    private final UserPersistenceMapper mapper;

    public PostgresUserRepositoryAdapter(SpringDataUserRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
    @Override public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(mapper::toDomain);
    }
    @Override public Optional<User> findById(UserId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }
    @Override public boolean existsByEmail(Email email) {
        return repository.existsByEmail(email.value());
    }
    @Override public User save(User user) {
        try {
            // Flush fuerza a detectar también duplicados concurrentes antes de emitir un token.
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(user)));
        } catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && violation.getConstraintName().toLowerCase(java.util.Locale.ROOT).contains("uk_iam_users_email")) {
                    throw new UserAlreadyExistsException();
                }
            }
            throw ex;
        }
    }
}
