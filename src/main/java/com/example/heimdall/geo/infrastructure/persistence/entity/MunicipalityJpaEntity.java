package com.example.heimdall.geo.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "geo_municipalities")
public class MunicipalityJpaEntity {

    @Id
    @Column(length = 5, nullable = false)
    private String code;

    @Column(name = "department_code", length = 2, nullable = false)
    private String departmentCode;

    @Column(length = 100, nullable = false)
    private String name;

    protected MunicipalityJpaEntity() {}

    public MunicipalityJpaEntity(String code, String departmentCode, String name) {
        this.code = code;
        this.departmentCode = departmentCode;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public String getName() {
        return name;
    }
}
