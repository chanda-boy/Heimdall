package com.example.heimdall.geo.domain.port.in;

import com.example.heimdall.geo.domain.model.DepartmentCode;
import com.example.heimdall.geo.domain.model.Municipality;
import java.util.List;

public interface ListMunicipalitiesByDepartmentUseCase {
    List<Municipality> listByDepartment(DepartmentCode departmentCode);
}
