package com.example.heimdall.geo.infrastructure.config;

import com.example.heimdall.geo.application.usecase.GeoCatalogQueryService;
import com.example.heimdall.geo.domain.port.out.DepartmentRepositoryPort;
import com.example.heimdall.geo.domain.port.out.MunicipalityRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GeoConfiguration {

    @Bean
    public GeoCatalogQueryService geoCatalogQueryService(DepartmentRepositoryPort departments,
                                                         MunicipalityRepositoryPort municipalities) {
        return new GeoCatalogQueryService(departments, municipalities);
    }
}
