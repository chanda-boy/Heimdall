package com.example.heimdall.geo.presentation.rest;

import com.example.heimdall.geo.application.usecase.GeoCatalogQueryService;
import com.example.heimdall.geo.domain.model.DepartmentCode;
import com.example.heimdall.geo.domain.model.MunicipalityCode;
import com.example.heimdall.geo.presentation.dto.response.DepartmentResponseDto;
import com.example.heimdall.geo.presentation.dto.response.MunicipalityResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/geo")
public class GeoCatalogController {

    private final GeoCatalogQueryService geoCatalog;

    public GeoCatalogController(GeoCatalogQueryService geoCatalog) {
        this.geoCatalog = Objects.requireNonNull(geoCatalog);
    }

    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentResponseDto>> listDepartments() {
        List<DepartmentResponseDto> result = geoCatalog.listAll()
                .stream()
                .map(DepartmentResponseDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/departments/{code}")
    public ResponseEntity<DepartmentResponseDto> getDepartment(@PathVariable String code) {
        var department = geoCatalog.getByCode(new DepartmentCode(code));
        return ResponseEntity.ok(DepartmentResponseDto.from(department));
    }

    @GetMapping("/departments/{code}/municipalities")
    public ResponseEntity<List<MunicipalityResponseDto>> listMunicipalities(@PathVariable String code) {
        List<MunicipalityResponseDto> result = geoCatalog.listByDepartment(new DepartmentCode(code))
                .stream()
                .map(MunicipalityResponseDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/municipalities/{code}")
    public ResponseEntity<MunicipalityResponseDto> getMunicipality(@PathVariable String code) {
        var municipality = geoCatalog.getByCode(new MunicipalityCode(code));
        return ResponseEntity.ok(MunicipalityResponseDto.from(municipality));
    }
}
