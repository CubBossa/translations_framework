package de.cubbossa.tinytranslations;

import de.cubbossa.tinytranslations.tinyobject.TinyObjectResolver;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Getter
@Setter
class MessageImpl implements Message {

    @Getter
    private final TranslationKey key;
    private final List<TranslationArgument> arguments = Collections.emptyList();
    private final Collection<TagResolver> resolvers = new LinkedList<>();
    @Setter
    private Supplier<Collection<TinyObjectResolver>> objectResolverSupplier = Collections::emptyList;
    private Style style = Style.empty();
    private List<Component> children = new ArrayList<>();
    private Map<Locale, String> dictionary;
    private String fallback;

    private Collection<PlaceholderDescription> placeholderDescriptions;
    private String comment;

    public MessageImpl(TranslationKey key) {
        this(key, key.asTranslationKey());
    }

    public MessageImpl(TranslationKey key, String fallback) {
        this.key = key;
        this.dictionary = new ConcurrentHashMap<>();
        this.dictionary.put(TinyTranslations.FALLBACK_DEFAULT_LOCALE, fallback);

        this.placeholderDescriptions = new LinkedList<>();
    }

    public MessageImpl(TranslationKey key, MessageImpl other) {
        this.key = key;
        this.style = other.style.color(other.style.color());
        this.children = other.children().stream().map(c -> c.children(c.children())).toList();
        this.dictionary = new HashMap<>(other.dictionary);
        this.fallback = other.fallback;
        this.placeholderDescriptions = new LinkedList<>(other.placeholderDescriptions);
        this.comment = other.comment;
        this.resolvers.addAll(other.resolvers);
        this.objectResolverSupplier = other.objectResolverSupplier;
    }

    @Override
    public String toString() {
        return "Message{key=\"" + getKey().asNamespacedKey() + "\"}";
    }

    @Override
    public String toString(MessageEncoding format) {
        return format.format(GlobalTranslator.render(asComponent(), Locale.ENGLISH));
    }

    @Override
    public String toString(MessageEncoding format, Locale locale) {
        return toString(format);
    }

    @Override
    public Collection<TagResolver> getResolvers() {
        return List.copyOf(resolvers);
    }

    @Override
    public Message formatted(TagResolver... resolver) {
        MessageImpl message = new MessageImpl(key, this);
        message.resolvers.addAll(List.of(resolver));
        return message;
    }

    @Override
    public Collection<TinyObjectResolver> getObjectResolversInScope() {
        return objectResolverSupplier.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageImpl message = (MessageImpl) o;
        return Objects.equals(key, message.key) && Objects.equals(arguments, message.arguments) && Objects.equals(style, message.style) && Objects.equals(children, message.children) && Objects.equals(dictionary, message.dictionary) && Objects.equals(fallback, message.fallback) && Objects.equals(placeholderDescriptions, message.placeholderDescriptions) && Objects.equals(comment, message.comment);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public Message clone() {
        return new MessageImpl(key, this);
    }

    @Override
    public int compareTo(@NotNull Message o) {
        return key().compareTo(o.key());
    }

    @Override
    public @NotNull String translationKey() {
        return key.asTranslationKey();
    }

    @Override
    public @NotNull String key() {
        return translationKey();
    }

    @Override
    public @NotNull TranslatableComponent key(@NotNull String key) {
        throw new IllegalArgumentException("Cannot change message key.");
    }

    @Override
    public @NotNull List<Component> args() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull List<TranslationArgument> arguments() {
        return arguments;
    }

    @Override
    public @NotNull TranslatableComponent arguments(@NotNull ComponentLike @NotNull ... args) {
        return new MessageImpl(key, this);
    }

    @Override
    public @NotNull TranslatableComponent arguments(@NotNull List<? extends ComponentLike> args) {
        return new MessageImpl(key, this);
    }

    @Override
    public @Nullable String fallback() {
        return fallback;
    }

    @Override
    public @NotNull TranslatableComponent fallback(@Nullable String fallback) {
        var clone = new MessageImpl(key, this);
        clone.fallback = fallback;
        return clone;
    }

    @Override
    public @NotNull Builder toBuilder() {
        return null;
    }

    @Override
    public @Unmodifiable @NotNull List<Component> children() {
        return new ArrayList<>(children);
    }

    @Override
    public @NotNull TranslatableComponent children(@NotNull List<? extends ComponentLike> children) {
        var clone = new MessageImpl(key, this);
        clone.children = children.stream().map(ComponentLike::asComponent).toList();
        return clone;
    }

    @Override
    public @NotNull Style style() {
        return style;
    }

    @Override
    public @NotNull TranslatableComponent style(@NotNull Style style) {
        var clone = new MessageImpl(key, this);
        clone.style = style;
        return clone;
    }

    public Map<String, Optional<String>> getPlaceholderDescriptions() {
        Map<String, Optional<String>> var = new HashMap<>();
        this.placeholderDescriptions.forEach(placeholderDescription -> {
            var.put(placeholderDescription.names()[0], Optional.ofNullable(placeholderDescription.description()));
        });
        return var;
    }

    @Override
    public void setPlaceholderDescriptions(Map<String, Optional<String>> placeholderDescriptions) {
        placeholderDescriptions.forEach((s, s2) -> {
            this.placeholderDescriptions.add(new PlaceholderDescription(new String[]{s}, s2.orElse(null), Object.class));
        });
    }

    @Override
    public Message comment(@Nullable String comment) {
        var clone = new MessageImpl(key, this);
        clone.comment = comment;
        return clone;
    }

    @Override
    public @Nullable String comment() {
        return comment;
    }

    @Override
    public Message dictionary(Map<Locale, String> dictionary) {
        var clone = new MessageImpl(key, this);
        clone.dictionary.clear();
        clone.dictionary.putAll(dictionary);
        return clone;
    }

    @Override
    public Map<Locale, String> dictionary() {
        return Collections.unmodifiableMap(dictionary);
    }

    @Override
    public Message placeholderDescriptions(Collection<PlaceholderDescription> descriptions) {
        var clone = new MessageImpl(key, this);
        clone.placeholderDescriptions.clear();
        clone.placeholderDescriptions.addAll(descriptions);
        return clone;
    }

    @Override
    public Collection<PlaceholderDescription> placeholderDescriptions() {
        return Collections.unmodifiableCollection(placeholderDescriptions);
    }
}
