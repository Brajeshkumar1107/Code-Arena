package com.codexsphere.codearena.tatasteel.whatsapp.dto;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Converts a Meta webhook message into the normalized inbound message model.
 */
@Component
public class WhatsAppInboundMapper {

    public InboundMessage from(WhatsAppWebhookPayload.Message message) {
        String type = message.type() == null ? "unknown" : message.type().toLowerCase(Locale.ROOT);

        return switch (type) {
            case "text" -> new InboundMessage(
                    message.from(),
                    message.id(),
                    InboundMessageType.TEXT,
                    body(message.text()),
                    null,
                    null,
                    null
            );
            case "image" -> new InboundMessage(
                    message.from(),
                    message.id(),
                    InboundMessageType.IMAGE,
                    message.image() == null ? null : message.image().caption(),
                    message.image() == null ? null : message.image().id(),
                    message.image() == null ? null : message.image().mimeType(),
                    null
            );
            case "document" -> new InboundMessage(
                    message.from(),
                    message.id(),
                    InboundMessageType.DOCUMENT,
                    message.document() == null ? null : message.document().caption(),
                    message.document() == null ? null : message.document().id(),
                    message.document() == null ? null : message.document().mimeType(),
                    message.document() == null ? null : message.document().filename()
            );
            case "interactive" -> fromInteractive(message);
            case "button" -> new InboundMessage(
                    message.from(),
                    message.id(),
                    InboundMessageType.BUTTON_REPLY,
                    message.button() == null ? null : message.button().text(),
                    null,
                    null,
                    null
            );
            default -> new InboundMessage(
                    message.from(),
                    message.id(),
                    InboundMessageType.UNKNOWN,
                    null,
                    null,
                    null,
                    null
            );
        };
    }

    private InboundMessage fromInteractive(WhatsAppWebhookPayload.Message message) {
        WhatsAppWebhookPayload.Interactive interactive = message.interactive();
        if (interactive == null) {
            return new InboundMessage(
                    message.from(), message.id(), InboundMessageType.UNKNOWN,
                    null, null, null, null
            );
        }

        if (interactive.buttonReply() != null) {
            return new InboundMessage(
                    message.from(), message.id(), InboundMessageType.BUTTON_REPLY,
                    interactive.buttonReply().title(), null, null, null
            );
        }

        if (interactive.listReply() != null) {
            return new InboundMessage(
                    message.from(), message.id(), InboundMessageType.LIST_REPLY,
                    interactive.listReply().title(), null, null, null
            );
        }

        return new InboundMessage(
                message.from(), message.id(), InboundMessageType.UNKNOWN,
                null, null, null, null
        );
    }

    private String body(WhatsAppWebhookPayload.Text text) {
        return text == null ? null : text.body();
    }

}
