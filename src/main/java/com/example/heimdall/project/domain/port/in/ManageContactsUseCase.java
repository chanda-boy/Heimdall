package com.example.heimdall.project.domain.port.in;

import com.example.heimdall.project.domain.model.Contact;
import com.example.heimdall.project.domain.model.ContactChannel;
import com.example.heimdall.project.domain.model.ContactId;
import com.example.heimdall.project.domain.model.ContactSubscription;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.model.SciPosition;
import java.util.List;
import java.util.UUID;

public interface ManageContactsUseCase {

    Contact addContact(
            ProjectId projectId,
            UUID userId,
            String fullName,
            SciPosition sciPosition,
            int priority,
            List<ContactChannel> channels,
            List<ContactSubscription> subscriptions,
            UUID requesterUserId,
            boolean isAdmin
    );

    Contact getContact(ContactId contactId, UUID requesterUserId, boolean isAdmin);

    List<Contact> listContacts(ProjectId projectId, UUID requesterUserId, boolean isAdmin);

    Contact updateContact(
            ContactId contactId,
            String fullName,
            SciPosition sciPosition,
            int priority,
            boolean active,
            UUID requesterUserId,
            boolean isAdmin
    );

    void deleteContact(ContactId contactId, UUID requesterUserId, boolean isAdmin);

    Contact updateSubscriptions(
            ContactId contactId,
            List<ContactSubscription> subscriptions,
            UUID requesterUserId,
            boolean isAdmin
    );

    Contact updateChannels(
            ContactId contactId,
            List<ContactChannel> channels,
            UUID requesterUserId,
            boolean isAdmin
    );
}
