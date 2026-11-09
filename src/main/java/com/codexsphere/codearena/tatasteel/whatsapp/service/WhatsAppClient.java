package com.codexsphere.codearena.tatasteel.whatsapp.service;

import com.codexsphere.codearena.tatasteel.whatsapp.dto.OutgoingMessage;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.OutgoingMessageType;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.ReplyButton;

import java.util.List;

/**
 * Abstraction over the outbound WhatsApp Cloud API.
 *
 * <p>Tests provide a mock implementation so no real messages are sent during
 * automated test runs.</p>
 */
public interface WhatsAppClient {

    void sendText(String to, String text);

    void sendButtons(String to, String body, List<ReplyButton> buttons);

    default void send(String to, OutgoingMessage message) {
        if (message.type() == OutgoingMessageType.BUTTONS) {
            sendButtons(to, message.text(), message.buttons());
        } else {
            sendText(to, message.text());
        }
    }

}
