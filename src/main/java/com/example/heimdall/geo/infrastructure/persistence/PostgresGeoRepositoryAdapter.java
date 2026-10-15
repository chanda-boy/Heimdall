package com.example.heimdall.geo.infrastructure.persistence;

import com.example.heimdall.geo.domain.model.Department;
import com.example.heimdall.geo.domain.model.DepartmentCode;
import com.example.heimdall.geo.domain.model.Municipality;
import com.example.heimdall.geo.domain.model.MunicipalityCode;
import com.example.heimdall.geo.domain.port.out.DepartmentRepositoryPort;
import com.example.heimdall.geo.domain.port.out.MunicipalityRepositoryPort;
import com.example.heimdall.geo.infrastructure.persistence.mapper.GeoPersistenceMapper;
import com.example.heimdall.geo.infrastructure.persistence.repository.SpringDataDepartmentRepository;
import com.example.heimdall.geo.infrastructure.persistence.repository.SpringDataMunicipalityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class PostgresGeoRepositoryAdapter implements DepartmentRepositoryPort, MunicipalityRepositoryPort {

    private final SpringDataDepartmentRepository departments;
    private final SpringDataMunicipalityRepository municipalities;

    public PostgresGeoRepositoryAdapter(SpringDataDepartmentRepository departments,
                                        SpringDataMunicipalityRepository municipalities) {
        this.departments = Objects.requireNonNull(departments);
        this.municipalities = Objects.requireNonNull(municipalities);
    }

    @Override
    public List<Department> findAll() {
        return departments.findAllByOrderByNameAsc()
                .stream()
                .map(GeoPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Department> findByCode(DepartmentCode code) {
        return departments.findById(code.value())
                .map(GeoPersistenceMapper::toDomain);
    }

    @Override
    public List<Municipality> findByDepartmentCode(DepartmentCode departmentCode) {
        return municipalities.findByDepartmentCodeOrderByNameAsc(departmentCode.value())
                .stream()
                .map(GeoPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Municipality> findByCode(MunicipalityCode code) {
        return municipalities.findById(code.value())
                .map(GeoPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByCode(MunicipalityCode code) {
        return municipalities.existsById(code.value());
    }
}
