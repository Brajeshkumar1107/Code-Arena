package com.codexsphere.codearena.tatasteel.whatsapp.dto;

/**
 * Normalized inbound WhatsApp message used by the chatbot.
 */
public record InboundMessage(
        String phoneNumber,
        String waMessageId,
        InboundMessageType type,
        String text,
        String mediaId,
        String mimeType,
        String filename
) {
}
