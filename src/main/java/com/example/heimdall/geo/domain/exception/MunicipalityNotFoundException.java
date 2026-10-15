package com.example.heimdall.geo.domain.exception;

import com.example.heimdall.geo.domain.model.MunicipalityCode;

public class MunicipalityNotFoundException extends RuntimeException {
    public MunicipalityNotFoundException(MunicipalityCode code) {
        super("Municipality not found with code: " + code.value());
    }
}
