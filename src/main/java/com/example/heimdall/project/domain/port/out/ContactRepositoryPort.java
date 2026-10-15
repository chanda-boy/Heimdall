package com.example.heimdall.project.domain.port.out;

import com.example.heimdall.project.domain.model.Contact;
import com.example.heimdall.project.domain.model.ContactId;
import com.example.heimdall.project.domain.model.ProjectId;
import java.util.List;
import java.util.Optional;

public interface ContactRepositoryPort {
    Contact save(Contact contact);
    Optional<Contact> findById(ContactId id);
    List<Contact> findByProjectId(ProjectId projectId);
    void delete(ContactId id);
}
