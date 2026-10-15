package com.example.heimdall.geo.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "geo_departments")
public class DepartmentJpaEntity {

    @Id
    @Column(length = 2, nullable = false)
    private String code;

    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    @Column(length = 100, nullable = false)
    private String name;

    protected DepartmentJpaEntity() {}

    public DepartmentJpaEntity(String code, String countryCode, String name) {
        this.code = code;
        this.countryCode = countryCode;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getName() {
        return name;
    }
}
