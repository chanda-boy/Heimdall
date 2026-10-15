package com.example.heimdall.geo.domain.port.out;

import com.example.heimdall.geo.domain.model.DepartmentCode;
import com.example.heimdall.geo.domain.model.Municipality;
import com.example.heimdall.geo.domain.model.MunicipalityCode;
import java.util.List;
import java.util.Optional;

public interface MunicipalityRepositoryPort {
    List<Municipality> findByDepartmentCode(DepartmentCode departmentCode);
    Optional<Municipality> findByCode(MunicipalityCode code);
    boolean existsByCode(MunicipalityCode code);
}
