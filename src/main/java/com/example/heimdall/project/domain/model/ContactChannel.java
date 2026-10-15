package com.example.heimdall.project.domain.model;

import com.example.heimdall.project.domain.exception.InvalidContactChannelException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record ContactChannel(UUID id, ChannelType channelType, String destination, boolean verified) {
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public ContactChannel {
        Objects.requireNonNull(id, "Channel ID is required");
        Objects.requireNonNull(channelType, "Channel type is required");
        Objects.requireNonNull(destination, "Destination is required");
        destination = destination.strip();

        if (channelType == ChannelType.WHATSAPP || channelType == ChannelType.SMS) {
            if (!E164_PATTERN.matcher(destination).matches()) {
                throw new InvalidContactChannelException(
                        "Phone destination must follow international E.164 format (e.g. +573013338304): " + destination);
            }
        } else if (channelType == ChannelType.EMAIL) {
            destination = destination.toLowerCase(Locale.ROOT);
            if (!EMAIL_PATTERN.matcher(destination).matches()) {
                throw new InvalidContactChannelException("Email destination must have a valid format: " + destination);
            }
        }
    }

    public static ContactChannel of(ChannelType type, String destination) {
        return new ContactChannel(UUID.randomUUID(), type, destination, false);
    }
}
