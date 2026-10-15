package com.example.heimdall.project.domain.model;

import com.example.heimdall.project.domain.exception.InvalidCoordinatesException;
import java.util.Objects;

public record Coordinates(double latitude, double longitude, CoordinatesSource source) {
    public Coordinates {
        Objects.requireNonNull(source, "Coordinates source is required");
        if (latitude < -90.0 || latitude > 90.0) {
            throw new InvalidCoordinatesException("Latitude must be between -90 and 90: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new InvalidCoordinatesException("Longitude must be between -180 and 180: " + longitude);
        }
    }

    public static Coordinates of(double latitude, double longitude) {
        return new Coordinates(latitude, longitude, CoordinatesSource.GEOCODED);
    }
}
