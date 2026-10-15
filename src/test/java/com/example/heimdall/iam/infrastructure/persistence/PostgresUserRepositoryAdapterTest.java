package com.example.heimdall.iam.infrastructure.persistence;

import com.example.heimdall.iam.domain.exception.UserAlreadyExistsException;
import com.example.heimdall.iam.domain.model.*;
import com.example.heimdall.iam.domain.port.out.UserRepositoryPort;
import com.example.heimdall.iam.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PostgresUserRepositoryAdapterTest {
    @Autowired UserRepositoryPort repository;
    @Autowired SpringDataUserRepository jpa;
    @BeforeEach void clear() { jpa.deleteAll(); }

    @Test void roundTripPreservesIdentityHashAndRole() {
        User saved = repository.save(user("operator@example.com"));
        assertThat(repository.findById(saved.id())).contains(saved);
        assertThat(repository.findByEmail(new Email("OPERATOR@EXAMPLE.COM"))).contains(saved);
        assertThat(repository.existsByEmail(saved.email())).isTrue();
    }
    @Test void simultaneousRegistrationsAreProtectedByDatabaseConstraint() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                start.await();
                try { repository.save(user("same@example.com")); return true; }
                catch (UserAlreadyExistsException expected) { return false; }
            };
            Future<Boolean> first = executor.submit(attempt);
            Future<Boolean> second = executor.submit(attempt);
            start.countDown();
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(jpa.count()).isEqualTo(1);
        }
    }
    private User user(String email) {
        return new User(UserId.random(), new Email(email), "$2a$hash-for-persistence-test", Role.USER);
    }
}
