package com.example.heimdall.iam;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DomainArchitectureTest {
    private static final Pattern IMPORT = Pattern.compile(
            "(?m)^\\s*import\\s+(?:static\\s+)?([\\w.]+)(?:\\.\\*)?\\s*;");
    private static final String DOMAIN_PACKAGE = "com.example.heimdall.iam.domain";

    @Test
    void domainImportsOnlyJdkAndItsOwnTypes() throws IOException {
        Path domainRoot = Path.of("src/main/java/com/example/heimdall/iam/domain");
        assertTrue(Files.isDirectory(domainRoot), "IAM domain source directory must exist");

        List<Path> sources;
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(domainRoot)) {
            sources = files.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertFalse(sources.isEmpty(), "IAM domain should contain source files");

        for (Path source : sources) {
            Matcher imports = IMPORT.matcher(Files.readString(source));
            while (imports.find()) {
                String importedType = imports.group(1);
                if (!importedType.startsWith("java.")
                        && !importedType.startsWith(DOMAIN_PACKAGE + ".")) {
                    violations.add(domainRoot.relativize(source) + " imports " + importedType);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "IAM domain must not depend on frameworks or outer layers: "
                + String.join(", ", violations));
    }

    @Test
    void geoDomainImportsOnlyJdkAndItsOwnTypes() throws IOException {
        Path domainRoot = Path.of("src/main/java/com/example/heimdall/geo/domain");
        assertTrue(Files.isDirectory(domainRoot), "GEO domain source directory must exist");

        List<Path> sources;
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(domainRoot)) {
            sources = files.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertFalse(sources.isEmpty(), "GEO domain should contain source files");

        String geoPackage = "com.example.heimdall.geo.domain";
        for (Path source : sources) {
            Matcher imports = IMPORT.matcher(Files.readString(source));
            while (imports.find()) {
                String importedType = imports.group(1);
                if (!importedType.startsWith("java.")
                        && !importedType.startsWith(geoPackage + ".")) {
                    violations.add(domainRoot.relativize(source) + " imports " + importedType);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "GEO domain must not depend on frameworks or outer layers: "
                + String.join(", ", violations));
    }

    @Test
    void projectDomainImportsOnlyJdkAndItsOwnTypes() throws IOException {
        Path domainRoot = Path.of("src/main/java/com/example/heimdall/project/domain");
        assertTrue(Files.isDirectory(domainRoot), "PROJECT domain source directory must exist");

        List<Path> sources;
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(domainRoot)) {
            sources = files.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertFalse(sources.isEmpty(), "PROJECT domain should contain source files");

        String projectPackage = "com.example.heimdall.project.domain";
        for (Path source : sources) {
            Matcher imports = IMPORT.matcher(Files.readString(source));
            while (imports.find()) {
                String importedType = imports.group(1);
                if (!importedType.startsWith("java.")
                        && !importedType.startsWith(projectPackage + ".")) {
                    violations.add(domainRoot.relativize(source) + " imports " + importedType);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "PROJECT domain must not depend on frameworks or outer layers: "
                + String.join(", ", violations));
    }
}
