package com.example.heimdall.geo.infrastructure.persistence.repository;

import com.example.heimdall.geo.infrastructure.persistence.entity.MunicipalityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataMunicipalityRepository extends JpaRepository<MunicipalityJpaEntity, String> {
    List<MunicipalityJpaEntity> findByDepartmentCodeOrderByNameAsc(String departmentCode);
}
