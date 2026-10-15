package com.example.heimdall.project.application.usecase;

import com.example.heimdall.project.domain.exception.ContactNotFoundException;
import com.example.heimdall.project.domain.exception.ProjectAccessDeniedException;
import com.example.heimdall.project.domain.model.AlertLevel;
import com.example.heimdall.project.domain.model.ChannelType;
import com.example.heimdall.project.domain.model.Contact;
import com.example.heimdall.project.domain.model.ContactChannel;
import com.example.heimdall.project.domain.model.ContactId;
import com.example.heimdall.project.domain.model.ContactSubscription;
import com.example.heimdall.project.domain.model.Coordinates;
import com.example.heimdall.project.domain.model.Hazard;
import com.example.heimdall.project.domain.model.MonitoringSchedule;
import com.example.heimdall.project.domain.model.NotificationPolicy;
import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.model.ProjectStatus;
import com.example.heimdall.project.domain.model.SciPosition;
import com.example.heimdall.project.domain.port.out.ContactRepositoryPort;
import com.example.heimdall.project.domain.port.out.ProjectRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContactManagementServiceTest {

    private ContactRepositoryPort contactRepository;
    private ProjectRepositoryPort projectRepository;
    private ContactManagementService service;

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID STRANGER_ID = UUID.randomUUID();
    private static final ProjectId PROJECT_ID = ProjectId.random();

    @BeforeEach
    void setUp() {
        contactRepository = mock(ContactRepositoryPort.class);
        projectRepository = mock(ProjectRepositoryPort.class);
        service = new ContactManagementService(contactRepository, projectRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        Project project = new Project(PROJECT_ID, OWNER_ID, null, "Mine", "CO", null,
                Coordinates.of(6.0, -75.0), ZoneId.of("America/Bogota"),
                MonitoringSchedule.defaultSchedule(), NotificationPolicy.defaultPolicy(),
                ProjectStatus.ACTIVE, NOW, NOW);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addContactSavesWithChannelsAndSubscriptions() {
        List<ContactChannel> channels = List.of(ContactChannel.of(ChannelType.WHATSAPP, "+573013338304"));
        List<ContactSubscription> subscriptions = List.of(new ContactSubscription(Hazard.FOREST_FIRE, AlertLevel.ORANGE));

        Contact contact = service.addContact(
                PROJECT_ID,
                null,
                "Jefe de Emergencias",
                SciPosition.SCI_CHIEF,
                1,
                channels,
                subscriptions,
                OWNER_ID,
                false
        );

        assertNotNull(contact);
        assertEquals("Jefe de Emergencias", contact.fullName());
        assertEquals(SciPosition.SCI_CHIEF, contact.sciPosition());
        assertEquals(1, contact.priority());
        assertEquals(1, contact.channels().size());
        assertEquals(1, contact.subscriptions().size());
        verify(contactRepository).save(any());
    }

    @Test
    void strangerCannotAddOrModifyContacts() {
        assertThrows(ProjectAccessDeniedException.class, () -> service.addContact(
                PROJECT_ID,
                null,
                "Jefe de Emergencias",
                SciPosition.SCI_CHIEF,
                1,
                List.of(),
                List.of(),
                STRANGER_ID,
                false
        ));
    }

    @Test
    void contactNotFoundThrowsException() {
        ContactId missing = ContactId.random();
        when(contactRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(ContactNotFoundException.class,
                () -> service.getContact(missing, OWNER_ID, false));
    }
}
