package de.cubbossa.translations.persistent;

import de.cubbossa.translations.Message;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public interface MessageStorage {

    Optional<String> readMessage(Message message, Locale locale);

    Map<Message, String> readMessages(Collection<Message> message, Locale locale);

    boolean writeMessage(Message message, Locale locale, String translation);

    Collection<Message> writeMessages(Collection<Message> messages, Locale locale);
}