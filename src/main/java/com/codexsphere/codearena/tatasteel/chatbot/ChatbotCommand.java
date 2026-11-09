package com.codexsphere.codearena.tatasteel.chatbot;

import java.util.Locale;

/**
 * Reserved commands available at any point in a conversation.
 */
public enum ChatbotCommand {
    MENU,
    HELP,
    BACK,
    CANCEL,
    START,
    NONE;

    public static ChatbotCommand from(String text) {
        if (text == null) {
            return NONE;
        }
        return switch (text.trim().toUpperCase(Locale.ROOT)) {
            case "MENU" -> MENU;
            case "HELP" -> HELP;
            case "BACK" -> BACK;
            case "CANCEL" -> CANCEL;
            case "START" -> START;
            default -> NONE;
        };
    }

}
