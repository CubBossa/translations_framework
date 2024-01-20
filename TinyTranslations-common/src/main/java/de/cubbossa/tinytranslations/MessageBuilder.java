package de.cubbossa.tinytranslations;

import de.cubbossa.tinytranslations.annotation.KeyPattern;
import de.cubbossa.tinytranslations.impl.MessageImpl;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.intellij.lang.annotations.Language;

import java.util.*;
import java.util.stream.Collectors;

public class MessageBuilder {

    private final MessageTranslator owner;
    private final String key;
    private final Map<Locale, String> translations;
    private final List<String> comments;
    private final Map<String, Placeholder> placeholderMap;

    public MessageBuilder(@KeyPattern String key) {
        this(null, key);
    }

    public MessageBuilder(MessageTranslator messageTranslator, @KeyPattern String key) {
        this.key = key;
        this.owner = messageTranslator;
        this.comments = new ArrayList<>();
        this.translations = new HashMap<>();
        this.placeholderMap = new HashMap<>();
    }

    public MessageBuilder withComment(String line) {
        this.comments.add(line);
        return this;
    }

    public MessageBuilder withDefault(@Language("NanoMessage") String defaultValue) {
        this.translations.put(TinyTranslations.DEFAULT_LOCALE, defaultValue);
        return this;
    }

    public MessageBuilder withTranslation(Locale locale, @Language("NanoMessage") String miniMessage) {
        return this.withTranslation(locale, MessageEncoding.MINI_MESSAGE, miniMessage);
    }

    public MessageBuilder withTranslation(Locale locale, MessageEncoding format, String translation) {
        this.translations.put(locale, format.wrap(translation));
        return this;
    }

    public MessageBuilder withPlaceholders(String... placeholders) {
        for (String placeholder : placeholders) {
            this.placeholderMap.put(placeholder, new Placeholder(placeholder, Optional.empty(), Optional.empty()));
        }
        return this;
    }

    public MessageBuilder withPlaceholder(String tag) {
        return withPlaceholder(tag, null, null);
    }

    public MessageBuilder withPlaceholder(String tag, String description) {
        return withPlaceholder(tag, description, null);
    }

    public MessageBuilder withPlaceholder(String tag, String description, TagResolver defaultResolver) {
        this.placeholderMap.put(tag, new Placeholder(tag, Optional.ofNullable(description), Optional.ofNullable(defaultResolver)));
        return this;
    }

    public Message build() {
        Message message = new MessageImpl(key);
        message.setComment(comments.isEmpty() ? null : String.join("\n", comments));
        message.getDictionary().putAll(translations);
        message.setPlaceholderTags(placeholderMap.values().stream()
                .collect(Collectors.toMap(Placeholder::tag, Placeholder::desc)));
        message = message.formatted(placeholderMap.values().stream()
                .map(Placeholder::resolver)
                .filter(Optional::isPresent).map(Optional::get)
                .toArray(TagResolver[]::new));
        if (owner != null) {
            owner.addMessage(message);
        }
        return message;
    }

    record Placeholder(String tag, Optional<String> desc, Optional<TagResolver> resolver) {
    }

}
