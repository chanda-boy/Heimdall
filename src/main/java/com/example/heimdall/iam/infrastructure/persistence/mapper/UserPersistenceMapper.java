package com.example.heimdall.iam.infrastructure.persistence.mapper;

import com.example.heimdall.iam.domain.model.*;
import com.example.heimdall.iam.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {
    public User toDomain(UserJpaEntity entity) {
        return new User(new UserId(entity.getId()), new Email(entity.getEmail()), entity.getPasswordHash(), entity.getRole());
    }
    public UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(user.id().value(), user.email().value(), user.passwordHash(), user.role());
    }
}
