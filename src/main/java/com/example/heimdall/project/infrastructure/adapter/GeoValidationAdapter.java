package com.example.heimdall.project.infrastructure.adapter;

import com.example.heimdall.geo.domain.model.MunicipalityCode;
import com.example.heimdall.geo.domain.port.out.MunicipalityRepositoryPort;
import com.example.heimdall.project.domain.port.out.GeoValidationPort;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class GeoValidationAdapter implements GeoValidationPort {

    private final MunicipalityRepositoryPort municipalityRepository;

    public GeoValidationAdapter(MunicipalityRepositoryPort municipalityRepository) {
        this.municipalityRepository = Objects.requireNonNull(municipalityRepository);
    }

    @Override
    public boolean isValidMunicipality(String municipalityCode) {
        if (municipalityCode == null || municipalityCode.length() != 5) {
            return false;
        }
        try {
            return municipalityRepository.existsByCode(new MunicipalityCode(municipalityCode));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
