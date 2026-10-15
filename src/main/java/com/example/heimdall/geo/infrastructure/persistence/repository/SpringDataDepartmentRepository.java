package com.example.heimdall.geo.infrastructure.persistence.repository;

import com.example.heimdall.geo.infrastructure.persistence.entity.DepartmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataDepartmentRepository extends JpaRepository<DepartmentJpaEntity, String> {
    List<DepartmentJpaEntity> findAllByOrderByNameAsc();
}
