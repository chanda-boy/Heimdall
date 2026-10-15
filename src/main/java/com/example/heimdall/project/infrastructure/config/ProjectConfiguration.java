package com.example.heimdall.project.infrastructure.config;

import com.example.heimdall.project.application.usecase.ContactManagementService;
import com.example.heimdall.project.application.usecase.ProjectManagementService;
import com.example.heimdall.project.domain.port.out.ContactRepositoryPort;
import com.example.heimdall.project.domain.port.out.GeoValidationPort;
import com.example.heimdall.project.domain.port.out.ProjectRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ProjectConfiguration {

    @Bean
    public ProjectManagementService projectManagementService(
            ProjectRepositoryPort projectRepository,
            GeoValidationPort geoValidation,
            Clock clock
    ) {
        return new ProjectManagementService(projectRepository, geoValidation, clock);
    }

    @Bean
    public ContactManagementService contactManagementService(
            ContactRepositoryPort contactRepository,
            ProjectRepositoryPort projectRepository,
            Clock clock
    ) {
        return new ContactManagementService(contactRepository, projectRepository, clock);
    }
}
