package com.example.heimdall.geo.presentation.dto.response;

import com.example.heimdall.geo.domain.model.Municipality;

public record MunicipalityResponseDto(String code, String departmentCode, String name) {
    public static MunicipalityResponseDto from(Municipality municipality) {
        return new MunicipalityResponseDto(
                municipality.code().value(),
                municipality.departmentCode().value(),
                municipality.name()
        );
    }
}
