package com.codexsphere.codearena.tatasteel.whatsapp.dto;

import java.util.List;

/**
 * A message produced by the chatbot that should be sent back to WhatsApp.
 */
public record OutgoingMessage(
        OutgoingMessageType type,
        String text,
        List<ReplyButton> buttons
) {

    public static OutgoingMessage text(String text) {
        return new OutgoingMessage(OutgoingMessageType.TEXT, text, List.of());
    }

    public static OutgoingMessage buttons(String text, List<ReplyButton> buttons) {
        return new OutgoingMessage(OutgoingMessageType.BUTTONS, text, List.copyOf(buttons));
    }

}
