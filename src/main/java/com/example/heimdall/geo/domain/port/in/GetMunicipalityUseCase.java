package com.example.heimdall.geo.domain.port.in;

import com.example.heimdall.geo.domain.model.Municipality;
import com.example.heimdall.geo.domain.model.MunicipalityCode;

public interface GetMunicipalityUseCase {
    Municipality getByCode(MunicipalityCode code);
}
