package com.codexsphere.codearena.tatasteel.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Inbound Meta WhatsApp Cloud API webhook payload.
 *
 * <p>Only the fields required by the MVP are modelled.  Unknown fields are
 * ignored so Meta can extend the payload without breaking this module.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayload(
        @JsonProperty("object") String object,
        @JsonProperty("entry") List<Entry> entry
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            @JsonProperty("id") String id,
            @JsonProperty("changes") List<Change> changes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(
            @JsonProperty("field") String field,
            @JsonProperty("value") Value value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            @JsonProperty("messaging_product") String messagingProduct,
            @JsonProperty("metadata") Metadata metadata,
            @JsonProperty("contacts") List<Contact> contacts,
            @JsonProperty("messages") List<Message> messages,
            @JsonProperty("statuses") List<Status> statuses
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            @JsonProperty("display_phone_number") String displayPhoneNumber,
            @JsonProperty("phone_number_id") String phoneNumberId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
            @JsonProperty("profile") Profile profile,
            @JsonProperty("wa_id") String waId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            @JsonProperty("name") String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("from") String from,
            @JsonProperty("id") String id,
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("type") String type,
            @JsonProperty("text") Text text,
            @JsonProperty("interactive") Interactive interactive,
            @JsonProperty("image") Media image,
            @JsonProperty("document") Media document,
            @JsonProperty("button") Button button
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Text(
            @JsonProperty("body") String body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Interactive(
            @JsonProperty("type") String type,
            @JsonProperty("button_reply") Reply buttonReply,
            @JsonProperty("list_reply") Reply listReply
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reply(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Media(
            @JsonProperty("id") String id,
            @JsonProperty("mime_type") String mimeType,
            @JsonProperty("caption") String caption,
            @JsonProperty("filename") String filename
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Button(
            @JsonProperty("text") String text,
            @JsonProperty("payload") String payload
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(
            @JsonProperty("id") String id,
            @JsonProperty("status") String status,
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("recipient_id") String recipientId,
            @JsonProperty("errors") List<StatusError> errors
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusError(
            @JsonProperty("code") Integer code,
            @JsonProperty("title") String title,
            @JsonProperty("message") String message
    ) {
    }

}
