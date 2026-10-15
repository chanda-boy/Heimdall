package com.example.heimdall.geo.domain.port.in;

import com.example.heimdall.geo.domain.model.Department;
import com.example.heimdall.geo.domain.model.DepartmentCode;

public interface GetDepartmentUseCase {
    Department getByCode(DepartmentCode code);
}
