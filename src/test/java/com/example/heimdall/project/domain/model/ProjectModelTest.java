package com.example.heimdall.project.domain.model;

import com.example.heimdall.project.domain.exception.InvalidContactChannelException;
import com.example.heimdall.project.domain.exception.InvalidCoordinatesException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectModelTest {

    @Test
    void coordinatesRejectOutOfBoundLatitudesAndLongitudes() {
        assertDoesNotThrow(() -> new Coordinates(6.2442, -75.5812, CoordinatesSource.GEOCODED));
        assertDoesNotThrow(() -> new Coordinates(-90.0, 180.0, CoordinatesSource.MANUAL));

        assertThrows(InvalidCoordinatesException.class, () -> new Coordinates(90.1, 0.0, CoordinatesSource.GEOCODED));
        assertThrows(InvalidCoordinatesException.class, () -> new Coordinates(-90.1, 0.0, CoordinatesSource.GEOCODED));
        assertThrows(InvalidCoordinatesException.class, () -> new Coordinates(0.0, 180.1, CoordinatesSource.GEOCODED));
        assertThrows(InvalidCoordinatesException.class, () -> new Coordinates(0.0, -180.1, CoordinatesSource.GEOCODED));
    }

    @Test
    void contactChannelsValidateE164AndEmailFormats() {
        // Valid E.164 numbers
        assertDoesNotThrow(() -> ContactChannel.of(ChannelType.WHATSAPP, "+573013338304"));
        assertDoesNotThrow(() -> ContactChannel.of(ChannelType.SMS, "+14155552671"));
        assertDoesNotThrow(() -> ContactChannel.of(ChannelType.EMAIL, "sci.chief@emergency.gov.co"));

        // Invalid formats
        assertThrows(InvalidContactChannelException.class,
                () -> ContactChannel.of(ChannelType.WHATSAPP, "3013338304")); // missing +
        assertThrows(InvalidContactChannelException.class,
                () -> ContactChannel.of(ChannelType.WHATSAPP, "+0123456789")); // invalid leading 0
        assertThrows(InvalidContactChannelException.class,
                () -> ContactChannel.of(ChannelType.SMS, "invalid-phone"));
        assertThrows(InvalidContactChannelException.class,
                () -> ContactChannel.of(ChannelType.EMAIL, "not-an-email"));
    }

    @Test
    void notificationPolicyDefaultsToLeadDays1AndOrange() {
        NotificationPolicy policy = NotificationPolicy.defaultPolicy();
        assertEquals(1, policy.notifyLeadDays());
        assertEquals(AlertLevel.ORANGE, policy.notifyMinLevel());

        assertThrows(IllegalArgumentException.class, () -> new NotificationPolicy(-1, AlertLevel.RED));
    }

    @Test
    void archivedProjectCannotBeModified() {
        Instant now = Instant.parse("2026-10-14T12:00:00Z");
        Project project = new Project(
                ProjectId.random(),
                UUID.randomUUID(),
                ProjectCode.of("PRJ-01"),
                "Mina Buriticá",
                "CO",
                "05113",
                Coordinates.of(6.9167, -75.9167),
                ZoneId.of("America/Bogota"),
                MonitoringSchedule.defaultSchedule(),
                NotificationPolicy.defaultPolicy(),
                ProjectStatus.ACTIVE,
                now,
                now
        );

        Project archived = project.changeStatus(ProjectStatus.ARCHIVED, now.plusSeconds(60));
        assertEquals(ProjectStatus.ARCHIVED, archived.status());

        assertThrows(IllegalStateException.class,
                () -> archived.updateDetails(ProjectCode.of("PRJ-02"), "Nuevo Nombre", "05001",
                        Coordinates.of(6.25, -75.56), ZoneId.of("America/Bogota"), now.plusSeconds(120)));

        assertThrows(IllegalStateException.class,
                () -> archived.updateSchedule(new MonitoringSchedule(LocalTime.of(8, 0), true), now.plusSeconds(120)));

        assertThrows(IllegalStateException.class,
                () -> archived.updateNotificationPolicy(new NotificationPolicy(2, AlertLevel.RED), now.plusSeconds(120)));
    }

    @Test
    void contactRequiresValidPriorityAndName() {
        ProjectId projectId = ProjectId.random();
        assertThrows(IllegalArgumentException.class,
                () -> new Contact(ContactId.random(), projectId, null, " ", SciPosition.SCI_CHIEF, 1, true, Instant.now(), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Contact(ContactId.random(), projectId, null, "Jefe SCI", SciPosition.SCI_CHIEF, 0, true, Instant.now(), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Contact(ContactId.random(), projectId, null, "Jefe SCI", SciPosition.SCI_CHIEF, -1, true, Instant.now(), List.of(), List.of()));

        Contact valid = new Contact(ContactId.random(), projectId, null, "  Jefe SCI  ", SciPosition.SCI_CHIEF, 1, true, Instant.now(), List.of(), List.of());
        assertEquals("Jefe SCI", valid.fullName());
    }
}
