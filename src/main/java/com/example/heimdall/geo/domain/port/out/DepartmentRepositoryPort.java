package com.example.heimdall.geo.domain.port.out;

import com.example.heimdall.geo.domain.model.Department;
import com.example.heimdall.geo.domain.model.DepartmentCode;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepositoryPort {
    List<Department> findAll();
    Optional<Department> findByCode(DepartmentCode code);
}
