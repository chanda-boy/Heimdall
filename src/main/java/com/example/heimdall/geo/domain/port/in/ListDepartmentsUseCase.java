package com.example.heimdall.geo.domain.port.in;

import com.example.heimdall.geo.domain.model.Department;
import java.util.List;

public interface ListDepartmentsUseCase {
    List<Department> listAll();
}
