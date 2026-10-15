package com.example.heimdall.project.application.usecase;

import com.example.heimdall.project.domain.exception.ContactNotFoundException;
import com.example.heimdall.project.domain.exception.ProjectAccessDeniedException;
import com.example.heimdall.project.domain.exception.ProjectNotFoundException;
import com.example.heimdall.project.domain.model.Contact;
import com.example.heimdall.project.domain.model.ContactChannel;
import com.example.heimdall.project.domain.model.ContactId;
import com.example.heimdall.project.domain.model.ContactSubscription;
import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.model.SciPosition;
import com.example.heimdall.project.domain.port.in.ManageContactsUseCase;
import com.example.heimdall.project.domain.port.out.ContactRepositoryPort;
import com.example.heimdall.project.domain.port.out.ProjectRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ContactManagementService implements ManageContactsUseCase {

    private final ContactRepositoryPort contactRepository;
    private final ProjectRepositoryPort projectRepository;
    private final Clock clock;

    public ContactManagementService(ContactRepositoryPort contactRepository,
                                  ProjectRepositoryPort projectRepository,
                                  Clock clock) {
        this.contactRepository = Objects.requireNonNull(contactRepository);
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Contact addContact(
            ProjectId projectId,
            UUID userId,
            String fullName,
            SciPosition sciPosition,
            int priority,
            List<ContactChannel> channels,
            List<ContactSubscription> subscriptions,
            UUID requesterUserId,
            boolean isAdmin
    ) {
        Project project = getAndValidateProject(projectId, requesterUserId, isAdmin);
        Instant now = clock.instant();
        Contact contact = new Contact(
                ContactId.random(),
                project.id(),
                userId,
                fullName,
                sciPosition,
                priority,
                true,
                now,
                channels,
                subscriptions
        );
        return contactRepository.save(contact);
    }

    @Override
    public Contact getContact(ContactId contactId, UUID requesterUserId, boolean isAdmin) {
        Objects.requireNonNull(contactId, "Contact ID is required");
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        getAndValidateProject(contact.projectId(), requesterUserId, isAdmin);
        return contact;
    }

    @Override
    public List<Contact> listContacts(ProjectId projectId, UUID requesterUserId, boolean isAdmin) {
        getAndValidateProject(projectId, requesterUserId, isAdmin);
        return contactRepository.findByProjectId(projectId);
    }

    @Override
    public Contact updateContact(
            ContactId contactId,
            String fullName,
            SciPosition sciPosition,
            int priority,
            boolean active,
            UUID requesterUserId,
            boolean isAdmin
    ) {
        Contact existing = getContact(contactId, requesterUserId, isAdmin);
        Contact updated = existing.withDetails(fullName, sciPosition, priority, active);
        return contactRepository.save(updated);
    }

    @Override
    public void deleteContact(ContactId contactId, UUID requesterUserId, boolean isAdmin) {
        Contact existing = getContact(contactId, requesterUserId, isAdmin);
        contactRepository.delete(existing.id());
    }

    @Override
    public Contact updateSubscriptions(
            ContactId contactId,
            List<ContactSubscription> subscriptions,
            UUID requesterUserId,
            boolean isAdmin
    ) {
        Contact existing = getContact(contactId, requesterUserId, isAdmin);
        Contact updated = existing.withSubscriptions(subscriptions);
        return contactRepository.save(updated);
    }

    @Override
    public Contact updateChannels(
            ContactId contactId,
            List<ContactChannel> channels,
            UUID requesterUserId,
            boolean isAdmin
    ) {
        Contact existing = getContact(contactId, requesterUserId, isAdmin);
        Contact updated = existing.withChannels(channels);
        return contactRepository.save(updated);
    }

    private Project getAndValidateProject(ProjectId projectId, UUID requesterUserId, boolean isAdmin) {
        Objects.requireNonNull(projectId, "Project ID is required");
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        if (!isAdmin && !project.ownerId().equals(requesterUserId)) {
            throw new ProjectAccessDeniedException(projectId, requesterUserId);
        }
        return project;
    }
}
