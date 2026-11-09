package com.codexsphere.codearena.tatasteel.chatbot;

import com.codexsphere.codearena.tatasteel.conversation.model.Conversation;
import com.codexsphere.codearena.tatasteel.conversation.model.ConversationState;
import com.codexsphere.codearena.tatasteel.conversation.model.ConversationStatus;
import com.codexsphere.codearena.tatasteel.conversation.model.ConversationWorkflow;
import com.codexsphere.codearena.tatasteel.customer.model.Customer;
import com.codexsphere.codearena.tatasteel.customer.service.CustomerService;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.InboundMessage;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.InboundMessageType;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.OutgoingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Rule based (hard-coded) chatbot for the first MVP.
 *
 * <p>No AI/LLM dependency is used.  The state machine is deliberately explicit
 * and backed by {@link Conversation} rows so conversations survive restarts.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private static final String MENU = """
            Please select an option:

            1. Register
            2. Submit Invoice
            3. Check Invoice Status
            4. My Rewards
            5. Help

            Reply with the number of your choice.
            """;

    private static final String WELCOME = "Welcome to Tata Steel!\n\n" + MENU;

    private static final String HELP = """
            Tata Steel Help

            1. Register - create your customer profile
            2. Submit Invoice - upload an invoice
            3. Check Invoice Status - track an invoice
            4. My Rewards - view your rewards
            5. Help - show this message

            Commands: MENU, HELP, BACK, CANCEL, START
            """;

    private static final String FALLBACK = """
            Sorry, I didn't understand that.

            Type MENU to see the available options, or HELP for assistance.
            """;

    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 100;
    private static final int CITY_MIN_LENGTH = 2;
    private static final int CITY_MAX_LENGTH = 120;

    private final CustomerService customerService;

    @Transactional
    public List<OutgoingMessage> handle(Conversation conversation, InboundMessage inbound) {
        String text = normalize(inbound);
        ChatbotCommand command = ChatbotCommand.from(text);
        log.debug(
                "Handling chatbot input for conversation id={} type={} command={} state={}.",
                conversation.getId(),
                inbound.type(),
                command,
                conversation.getState()
        );

        return switch (command) {
            case START -> startConversation(conversation);
            case MENU -> showMenu(conversation);
            case HELP -> showHelp(conversation);
            case CANCEL -> cancel(conversation);
            case BACK -> back(conversation);
            case NONE -> handleByState(conversation, inbound, text);
        };
    }

    private List<OutgoingMessage> handleByState(
            Conversation conversation,
            InboundMessage inbound,
            String text
    ) {
        if (isGreeting(text)) {
            return startConversation(conversation);
        }

        return switch (conversation.getState()) {
            case NEW -> startConversation(conversation);
            case MAIN_MENU -> handleMenu(conversation, inbound, text);
            case REGISTRATION_NAME -> handleRegistrationName(conversation, inbound, text);
            case REGISTRATION_CITY -> handleRegistrationCity(conversation, inbound, text);
            case INVOICE_UPLOAD -> handleInvoiceUpload(conversation, inbound);
            case INVOICE_REFERENCE -> handleInvoiceReference(conversation, inbound, text);
        };
    }

    private List<OutgoingMessage> startConversation(Conversation conversation) {
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        conversation.setState(ConversationState.MAIN_MENU);
        conversation.attributesMap().clear();
        return reply(WELCOME);
    }

    private List<OutgoingMessage> showMenu(Conversation conversation) {
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        conversation.setState(ConversationState.MAIN_MENU);
        return reply(MENU);
    }

    private List<OutgoingMessage> showHelp(Conversation conversation) {
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        conversation.setState(ConversationState.MAIN_MENU);
        return reply(HELP);
    }

    private List<OutgoingMessage> cancel(Conversation conversation) {
        conversation.setStatus(ConversationStatus.CANCELLED);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        conversation.setState(ConversationState.NEW);
        conversation.attributesMap().clear();
        return reply("Cancelled. Type START or MENU whenever you want to begin again.");
    }

    private List<OutgoingMessage> back(Conversation conversation) {
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        conversation.setState(ConversationState.MAIN_MENU);
        return reply("Okay, back to the main menu.\n\n" + MENU);
    }

    private List<OutgoingMessage> handleMenu(
            Conversation conversation,
            InboundMessage inbound,
            String text
    ) {
        if (inbound.type() == InboundMessageType.IMAGE || inbound.type() == InboundMessageType.DOCUMENT) {
            return reply("Please reply with a menu number between 1 and 5.\n\n" + MENU);
        }

        return switch (text.trim()) {
            case "1" -> startRegistration(conversation);
            case "2" -> startInvoiceSubmission(conversation);
            case "3" -> startInvoiceStatus(conversation);
            case "4" -> showRewards(conversation);
            case "5" -> showHelp(conversation);
            default -> reply(FALLBACK);
        };
    }

    private List<OutgoingMessage> startRegistration(Conversation conversation) {
        Optional<Customer> existing = customerService.findByPhone(conversation.getPhoneNumber());
        if (existing.isPresent()) {
            log.debug(
                    "Registration skipped for conversation id={}; customer id={} already exists.",
                    conversation.getId(),
                    existing.get().getId()
            );
            conversation.setState(ConversationState.MAIN_MENU);
            conversation.setWorkflow(ConversationWorkflow.GENERAL);
            conversation.setSelectedOption(null);
            return reply("You are already registered, " + existing.get().getName() + ".\n\n" + MENU);
        }

        log.debug("Starting registration for conversation id={}.", conversation.getId());
        conversation.setState(ConversationState.REGISTRATION_NAME);
        conversation.setWorkflow(ConversationWorkflow.REGISTRATION);
        conversation.setSelectedOption(1);
        conversation.attributesMap().clear();
        return reply("Please enter your name.\n\nType CANCEL to go back.");
    }

    private List<OutgoingMessage> handleRegistrationName(
            Conversation conversation,
            InboundMessage inbound,
            String text
    ) {
        if (!isTextLike(inbound)) {
            return reply("Please enter your name as text.");
        }

        String name = text.trim();
        if (name.length() < NAME_MIN_LENGTH || name.length() > NAME_MAX_LENGTH) {
            return reply("Please enter a valid name between 2 and 100 characters.");
        }

        conversation.attributesMap().put("name", name);
        conversation.setState(ConversationState.REGISTRATION_CITY);
        log.debug("Registration name captured for conversation id={}.", conversation.getId());
        return reply("Thanks, " + name + ". Please enter your city.");
    }

    private List<OutgoingMessage> handleRegistrationCity(
            Conversation conversation,
            InboundMessage inbound,
            String text
    ) {
        if (!isTextLike(inbound)) {
            return reply("Please enter your city as text.");
        }

        String city = text.trim();
        if (city.length() < CITY_MIN_LENGTH || city.length() > CITY_MAX_LENGTH) {
            return reply("Please enter a valid city between 2 and 120 characters.");
        }

        String name = conversation.attributesMap().get("name");
        if (name == null || name.isBlank()) {
            conversation.setState(ConversationState.REGISTRATION_NAME);
            return reply("Please enter your name first.");
        }

        CustomerService.RegistrationResult result =
                customerService.register(conversation.getPhoneNumber(), name, city);

        conversation.setCustomerId(result.customer().getId());
        conversation.setState(ConversationState.MAIN_MENU);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.attributesMap().clear();

        log.info(
                "Chatbot registration finished for conversation id={} customerId={} created={}.",
                conversation.getId(),
                result.customer().getId(),
                result.created()
        );
        if (result.created()) {
            return reply("Registration completed successfully, " + name + ".\n\n" + MENU);
        }
        return reply("You are already registered, " + result.customer().getName() + ".\n\n" + MENU);
    }

    private List<OutgoingMessage> startInvoiceSubmission(Conversation conversation) {
        conversation.setState(ConversationState.INVOICE_UPLOAD);
        conversation.setWorkflow(ConversationWorkflow.INVOICE_SUBMISSION);
        conversation.setSelectedOption(2);
        return reply("Please upload your invoice as an image or PDF.\n\nType CANCEL to go back.");
    }

    private List<OutgoingMessage> handleInvoiceUpload(
            Conversation conversation,
            InboundMessage inbound
    ) {
        if (inbound.type() == InboundMessageType.IMAGE || inbound.type() == InboundMessageType.DOCUMENT) {
            String reference = inbound.mediaId() == null ? "unavailable" : inbound.mediaId();
            log.info(
                    "Invoice document acknowledged for conversation id={} type={} mediaId={}.",
                    conversation.getId(),
                    inbound.type(),
                    reference
            );

            conversation.setState(ConversationState.MAIN_MENU);
            conversation.setWorkflow(ConversationWorkflow.GENERAL);
            conversation.setSelectedOption(null);
            conversation.attributesMap().clear();

            return reply("""
                    Invoice document received.

                    Document reference: %s

                    Invoice verification and reward processing will be enabled in the next release.

                    %s
                    """.formatted(reference, MENU));
        }

        if (isTextLike(inbound)) {
            return reply("Please upload your invoice as an image or PDF, or type CANCEL to go back.");
        }

        return reply("Unsupported message type. Please upload an invoice image or PDF, or type CANCEL to go back.");
    }

    private List<OutgoingMessage> startInvoiceStatus(Conversation conversation) {
        conversation.setState(ConversationState.INVOICE_REFERENCE);
        conversation.setWorkflow(ConversationWorkflow.INVOICE_STATUS);
        conversation.setSelectedOption(3);
        return reply("Please enter your invoice reference number (for example TS-INV-0001).\n\nType CANCEL to go back.");
    }

    private List<OutgoingMessage> handleInvoiceReference(
            Conversation conversation,
            InboundMessage inbound,
            String text
    ) {
        if (!isTextLike(inbound)) {
            return reply("Please enter your invoice reference number as text.");
        }

        String reference = text.trim().toUpperCase(Locale.ROOT);
        if (!reference.matches("TS-INV-\\d{4,}")) {
            return reply("Please enter a valid invoice reference number, for example TS-INV-0001.");
        }

        conversation.setState(ConversationState.MAIN_MENU);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        conversation.attributesMap().clear();

        log.debug(
                "Invoice status lookup requested for conversation id={} reference={} (not implemented yet).",
                conversation.getId(),
                reference
        );
        return reply("Invoice " + reference + " status lookup will be enabled in the next release.\n\n" + MENU);
    }

    private List<OutgoingMessage> showRewards(Conversation conversation) {
        conversation.setState(ConversationState.MAIN_MENU);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        return reply("""
                Rewards overview:

                Silver coin balance: 0
                Lucky draw coupons: 0

                Reward management will be enabled after invoice verification is available.

                %s
                """.formatted(MENU));
    }

    private boolean isTextLike(InboundMessage inbound) {
        return inbound.type() == InboundMessageType.TEXT
                || inbound.type() == InboundMessageType.BUTTON_REPLY
                || inbound.type() == InboundMessageType.LIST_REPLY;
    }

    private boolean isGreeting(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("hi")
                || normalized.equals("hello")
                || normalized.equals("hey")
                || normalized.equals("hii")
                || normalized.equals("namaste")
                || normalized.startsWith("hi ")
                || normalized.startsWith("hello ");
    }

    private String normalize(InboundMessage inbound) {
        return inbound.text() == null ? "" : inbound.text().trim();
    }

    private List<OutgoingMessage> reply(String text) {
        return List.of(OutgoingMessage.text(text));
    }

}
