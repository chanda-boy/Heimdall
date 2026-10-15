package com.example.heimdall.geo.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GeoHttpIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listDepartmentsReturnsSeedDataOrdered() throws Exception {
        mvc.perform(get("/api/v1/geo/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$[0].name").value("Antioquia"))
                .andExpect(jsonPath("$[0].code").value("05"))
                .andExpect(jsonPath("$[0].countryCode").value("CO"));
    }

    @Test
    void getDepartmentReturnsDetailOrNotFound() throws Exception {
        mvc.perform(get("/api/v1/geo/departments/05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("05"))
                .andExpect(jsonPath("$.name").value("Antioquia"));

        mvc.perform(get("/api/v1/geo/departments/99"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/geo/departments/ABC"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMunicipalitiesByDepartmentReturnsFilteredSeedData() throws Exception {
        mvc.perform(get("/api/v1/geo/departments/05/municipalities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Buriticá"))
                .andExpect(jsonPath("$[0].code").value("05113"))
                .andExpect(jsonPath("$[1].name").value("Medellín"))
                .andExpect(jsonPath("$[1].code").value("05001"));
    }

    @Test
    void getMunicipalityReturnsDetailOrNotFound() throws Exception {
        mvc.perform(get("/api/v1/geo/municipalities/05113"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("05113"))
                .andExpect(jsonPath("$.departmentCode").value("05"))
                .andExpect(jsonPath("$.name").value("Buriticá"));

        mvc.perform(get("/api/v1/geo/municipalities/99999"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/geo/municipalities/12"))
                .andExpect(status().isBadRequest());
    }
}
