package com.example.heimdall.geo.application.usecase;

import com.example.heimdall.geo.domain.exception.DepartmentNotFoundException;
import com.example.heimdall.geo.domain.exception.MunicipalityNotFoundException;
import com.example.heimdall.geo.domain.model.Department;
import com.example.heimdall.geo.domain.model.DepartmentCode;
import com.example.heimdall.geo.domain.model.Municipality;
import com.example.heimdall.geo.domain.model.MunicipalityCode;
import com.example.heimdall.geo.domain.port.in.GetDepartmentUseCase;
import com.example.heimdall.geo.domain.port.in.GetMunicipalityUseCase;
import com.example.heimdall.geo.domain.port.in.ListDepartmentsUseCase;
import com.example.heimdall.geo.domain.port.in.ListMunicipalitiesByDepartmentUseCase;
import com.example.heimdall.geo.domain.port.out.DepartmentRepositoryPort;
import com.example.heimdall.geo.domain.port.out.MunicipalityRepositoryPort;
import java.util.List;
import java.util.Objects;

public final class GeoCatalogQueryService implements
        ListDepartmentsUseCase,
        GetDepartmentUseCase,
        ListMunicipalitiesByDepartmentUseCase,
        GetMunicipalityUseCase {

    private final DepartmentRepositoryPort departments;
    private final MunicipalityRepositoryPort municipalities;

    public GeoCatalogQueryService(DepartmentRepositoryPort departments, MunicipalityRepositoryPort municipalities) {
        this.departments = Objects.requireNonNull(departments, "Department repository is required");
        this.municipalities = Objects.requireNonNull(municipalities, "Municipality repository is required");
    }

    @Override
    public List<Department> listAll() {
        return departments.findAll();
    }

    @Override
    public Department getByCode(DepartmentCode code) {
        Objects.requireNonNull(code, "Department code is required");
        return departments.findByCode(code)
                .orElseThrow(() -> new DepartmentNotFoundException(code));
    }

    @Override
    public List<Municipality> listByDepartment(DepartmentCode departmentCode) {
        Objects.requireNonNull(departmentCode, "Department code is required");
        // Check department exists
        getByCode(departmentCode);
        return municipalities.findByDepartmentCode(departmentCode);
    }

    @Override
    public Municipality getByCode(MunicipalityCode code) {
        Objects.requireNonNull(code, "Municipality code is required");
        return municipalities.findByCode(code)
                .orElseThrow(() -> new MunicipalityNotFoundException(code));
    }
}
