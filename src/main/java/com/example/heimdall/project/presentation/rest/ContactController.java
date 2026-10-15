package com.example.heimdall.project.presentation.rest;

import com.example.heimdall.project.application.usecase.ContactManagementService;
import com.example.heimdall.project.domain.model.Contact;
import com.example.heimdall.project.domain.model.ContactChannel;
import com.example.heimdall.project.domain.model.ContactId;
import com.example.heimdall.project.domain.model.ContactSubscription;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.presentation.dto.request.ContactChannelDto;
import com.example.heimdall.project.presentation.dto.request.ContactSubscriptionDto;
import com.example.heimdall.project.presentation.dto.request.CreateContactRequestDto;
import com.example.heimdall.project.presentation.dto.request.UpdateContactRequestDto;
import com.example.heimdall.project.presentation.dto.response.ContactResponseDto;
import com.example.heimdall.project.presentation.security.SecurityUserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/contacts")
public class ContactController {

    private final ContactManagementService contactService;

    public ContactController(ContactManagementService contactService) {
        this.contactService = Objects.requireNonNull(contactService);
    }

    @PostMapping
    public ResponseEntity<ContactResponseDto> addContact(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateContactRequestDto request
    ) {
        SecurityUserContext user = SecurityUserContext.current();

        List<ContactChannel> channels = request.channels() == null ? List.of() :
                request.channels().stream().map(ContactChannelDto::toDomain).toList();

        List<ContactSubscription> subscriptions = request.subscriptions() == null ? List.of() :
                request.subscriptions().stream().map(ContactSubscriptionDto::toDomain).toList();

        Contact created = contactService.addContact(
                new ProjectId(projectId),
                request.userId(),
                request.fullName(),
                request.sciPosition(),
                request.resolvePriority(),
                channels,
                subscriptions,
                user.userId(),
                user.isAdmin()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactResponseDto.from(created));
    }

    @GetMapping
    public ResponseEntity<List<ContactResponseDto>> listContacts(@PathVariable UUID projectId) {
        SecurityUserContext user = SecurityUserContext.current();
        List<ContactResponseDto> contacts = contactService.listContacts(new ProjectId(projectId), user.userId(), user.isAdmin())
                .stream()
                .map(ContactResponseDto::from)
                .toList();
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/{contactId}")
    public ResponseEntity<ContactResponseDto> getContact(
            @PathVariable UUID projectId,
            @PathVariable UUID contactId
    ) {
        SecurityUserContext user = SecurityUserContext.current();
        Contact contact = contactService.getContact(new ContactId(contactId), user.userId(), user.isAdmin());
        return ResponseEntity.ok(ContactResponseDto.from(contact));
    }

    @PutMapping("/{contactId}")
    public ResponseEntity<ContactResponseDto> updateContact(
            @PathVariable UUID projectId,
            @PathVariable UUID contactId,
            @Valid @RequestBody UpdateContactRequestDto request
    ) {
        SecurityUserContext user = SecurityUserContext.current();
        Contact updated = contactService.updateContact(
                new ContactId(contactId),
                request.fullName(),
                request.sciPosition(),
                request.resolvePriority(),
                request.resolveActive(),
                user.userId(),
                user.isAdmin()
        );
        return ResponseEntity.ok(ContactResponseDto.from(updated));
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> deleteContact(
            @PathVariable UUID projectId,
            @PathVariable UUID contactId
    ) {
        SecurityUserContext user = SecurityUserContext.current();
        contactService.deleteContact(new ContactId(contactId), user.userId(), user.isAdmin());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{contactId}/subscriptions")
    public ResponseEntity<ContactResponseDto> updateSubscriptions(
            @PathVariable UUID projectId,
            @PathVariable UUID contactId,
            @Valid @RequestBody List<ContactSubscriptionDto> request
    ) {
        SecurityUserContext user = SecurityUserContext.current();
        List<ContactSubscription> subs = request.stream().map(ContactSubscriptionDto::toDomain).toList();
        Contact updated = contactService.updateSubscriptions(new ContactId(contactId), subs, user.userId(), user.isAdmin());
        return ResponseEntity.ok(ContactResponseDto.from(updated));
    }

    @PutMapping("/{contactId}/channels")
    public ResponseEntity<ContactResponseDto> updateChannels(
            @PathVariable UUID projectId,
            @PathVariable UUID contactId,
            @Valid @RequestBody List<ContactChannelDto> request
    ) {
        SecurityUserContext user = SecurityUserContext.current();
        List<ContactChannel> channels = request.stream().map(ContactChannelDto::toDomain).toList();
        Contact updated = contactService.updateChannels(new ContactId(contactId), channels, user.userId(), user.isAdmin());
        return ResponseEntity.ok(ContactResponseDto.from(updated));
    }
}
