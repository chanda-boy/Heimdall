package com.example.heimdall.project.infrastructure.persistence;

import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectCode;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.port.out.ProjectRepositoryPort;
import com.example.heimdall.project.infrastructure.persistence.entity.ProjectJpaEntity;
import com.example.heimdall.project.infrastructure.persistence.mapper.ProjectPersistenceMapper;
import com.example.heimdall.project.infrastructure.persistence.repository.SpringDataProjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresProjectRepositoryAdapter implements ProjectRepositoryPort {

    private final SpringDataProjectRepository repository;

    public PostgresProjectRepositoryAdapter(SpringDataProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Project save(Project project) {
        ProjectJpaEntity entity = ProjectPersistenceMapper.toEntity(project);
        ProjectJpaEntity saved = repository.save(entity);
        return ProjectPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        return repository.findById(id.value())
                .map(ProjectPersistenceMapper::toDomain);
    }

    @Override
    public List<Project> findByOwnerId(UUID ownerId) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(ProjectPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByOwnerIdAndCode(UUID ownerId, ProjectCode code) {
        if (code == null || code.value() == null) return false;
        return repository.existsByOwnerIdAndCode(ownerId, code.value());
    }

    @Override
    public void delete(ProjectId id) {
        repository.deleteById(id.value());
    }
}
