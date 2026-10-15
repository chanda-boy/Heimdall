package com.example.heimdall.geo.domain.model;

import com.example.heimdall.geo.domain.exception.InvalidGeoCodeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoModelTest {

    @Test
    void departmentCodeValidatesTwoDigits() {
        DepartmentCode code = new DepartmentCode("05");
        assertEquals("05", code.value());

        assertThrows(InvalidGeoCodeException.class, () -> new DepartmentCode("5"));
        assertThrows(InvalidGeoCodeException.class, () -> new DepartmentCode("005"));
        assertThrows(InvalidGeoCodeException.class, () -> new DepartmentCode("AB"));
        assertThrows(NullPointerException.class, () -> new DepartmentCode(null));
    }

    @Test
    void municipalityCodeValidatesFiveDigitsAndExtractsDepartment() {
        MunicipalityCode code = new MunicipalityCode("05113");
        assertEquals("05113", code.value());
        assertEquals(new DepartmentCode("05"), code.departmentCode());

        assertThrows(InvalidGeoCodeException.class, () -> new MunicipalityCode("113"));
        assertThrows(InvalidGeoCodeException.class, () -> new MunicipalityCode("051130"));
        assertThrows(InvalidGeoCodeException.class, () -> new MunicipalityCode("ABCDE"));
        assertThrows(NullPointerException.class, () -> new MunicipalityCode(null));
    }

    @Test
    void departmentAndMunicipalityRejectBlankNames() {
        DepartmentCode deptCode = new DepartmentCode("05");
        MunicipalityCode munCode = new MunicipalityCode("05001");

        assertThrows(IllegalArgumentException.class, () -> new Department(deptCode, "CO", "  "));
        assertThrows(IllegalArgumentException.class, () -> new Municipality(munCode, deptCode, "  "));

        Department dept = new Department(deptCode, "co", " Antioquia ");
        assertEquals("CO", dept.countryCode());
        assertEquals("Antioquia", dept.name());

        Municipality mun = new Municipality(munCode, deptCode, " Medellín ");
        assertEquals("Medellín", mun.name());
    }
}
