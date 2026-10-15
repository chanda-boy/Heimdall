package com.example.heimdall.geo.infrastructure.persistence.mapper;

import com.example.heimdall.geo.domain.model.Department;
import com.example.heimdall.geo.domain.model.DepartmentCode;
import com.example.heimdall.geo.domain.model.Municipality;
import com.example.heimdall.geo.domain.model.MunicipalityCode;
import com.example.heimdall.geo.infrastructure.persistence.entity.DepartmentJpaEntity;
import com.example.heimdall.geo.infrastructure.persistence.entity.MunicipalityJpaEntity;

public final class GeoPersistenceMapper {

    private GeoPersistenceMapper() {}

    public static Department toDomain(DepartmentJpaEntity entity) {
        if (entity == null) return null;
        return new Department(
                new DepartmentCode(entity.getCode()),
                entity.getCountryCode(),
                entity.getName()
        );
    }

    public static DepartmentJpaEntity toEntity(Department department) {
        if (department == null) return null;
        return new DepartmentJpaEntity(
                department.code().value(),
                department.countryCode(),
                department.name()
        );
    }

    public static Municipality toDomain(MunicipalityJpaEntity entity) {
        if (entity == null) return null;
        return new Municipality(
                new MunicipalityCode(entity.getCode()),
                new DepartmentCode(entity.getDepartmentCode()),
                entity.getName()
        );
    }

    public static MunicipalityJpaEntity toEntity(Municipality municipality) {
        if (municipality == null) return null;
        return new MunicipalityJpaEntity(
                municipality.code().value(),
                municipality.departmentCode().value(),
                municipality.name()
        );
    }
}
