package com.example.heimdall.project.domain.exception;

import com.example.heimdall.project.domain.model.ContactId;

public class ContactNotFoundException extends RuntimeException {
    public ContactNotFoundException(ContactId id) {
        super("Contact not found with id: " + id.value());
    }
}
