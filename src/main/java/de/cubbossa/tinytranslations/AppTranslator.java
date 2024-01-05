package de.cubbossa.tinytranslations;

import de.cubbossa.tinytranslations.annotation.AppPathPattern;
import de.cubbossa.tinytranslations.annotation.AppPattern;
import de.cubbossa.tinytranslations.nanomessage.TranslationsPreprocessor;
import de.cubbossa.tinytranslations.persistent.MessageStorage;
import de.cubbossa.tinytranslations.persistent.StyleStorage;
import de.cubbossa.tinytranslations.nanomessage.DefaultResolvers;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;

@Getter
@Setter
public class AppTranslator implements Translator {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Translator parent;
    private final @AppPattern String name;
    private final Map<String, Translator> children;

    private TagResolver styleResolverCache = null;

    private Function<@Nullable Audience, @NotNull Locale> localeProvider = null;

    private final Map<String, Message> messageSet;
    private final StyleSet styleSet;
    private @Nullable MessageStorage messageStorage;
    private @Nullable StyleStorage styleStorage;

    private ReadWriteLock lock;
    private StyleDeserializer styleDeserializer;
    private TagResolver defaultResolvers;
    private TranslationsPreprocessor preprocessor;

    public AppTranslator(Translator parent, String name) {
        this.parent = parent;
        this.name = name;

        this.children = new ConcurrentHashMap<>();

        this.messageStorage = null;
        this.styleStorage = null;
        this.preprocessor = new TranslationsPreprocessor();

        this.messageSet = new HashMap<>() {
            @Override
            public Message put(String key, Message value) {
                value.setTranslator(AppTranslator.this);
                return super.put(key, value);
            }
        };
        this.styleSet = new StyleSet() {
            @Override
            public MessageStyle put(String key, MessageStyle value) {
                styleResolverCache = null;
                return super.put(key, value);
            }

            @Override
            public boolean remove(Object key, Object value) {
                styleResolverCache = null;
                return super.remove(key, value);
            }
        };

        this.defaultResolvers = TagResolver.resolver(
                DefaultResolvers.choice("choice"),
                DefaultResolvers.darken("darker"),
                DefaultResolvers.lighten("brighter"),
                DefaultResolvers.repeat("repeat"),
                DefaultResolvers.reverse("reverse"),
                DefaultResolvers.upper("upper"),
                DefaultResolvers.lower("lower"),
                DefaultResolvers.shortUrl("shorturl"),
                DefaultResolvers.preview("shorten"),
                MessageFormat.NBT.getTagResolver(),
                MessageFormat.LEGACY_PARAGRAPH.getTagResolver(),
                MessageFormat.LEGACY_AMPERSAND.getTagResolver(),
                MessageFormat.PLAIN.getTagResolver()
        );
    }

    @Override
    public @AppPathPattern String getPath() {
        if (parent == null) {
            return name;
        }
        return parent.getPath() + "." + name;
    }

    @Override
    public void close() {
        new HashMap<>(children).forEach((s, translations) -> translations.close());
        if (parent != null) {
            parent.remove(this.name);
        }
    }

    public void remove(String application) {
        var c = children.remove(application);
        if (c != null) {
            c.close();
        }
    }

    @Override
    public Translator fork(String name) {
        if (children.containsKey(name)) {
            throw new IllegalArgumentException("Another fork with name '" + name + "' already exists.");
        }

        Translator child = new AppTranslator(this, name);
        children.put(name, child);
        return child;
    }

    @Override
    public Translator forkWithStorage(String name) {
        Translator child = fork(name);
        child.setMessageStorage(messageStorage);
        child.setStyleStorage(styleStorage);
        return child;
    }

    @Override
    public Message message(String key) {
        return new MessageCore(this, key);
    }

    @Override
    public MessageBuilder messageBuilder(String key) {
        return new MessageBuilder(this, key);
    }

    @Override
    public Component process(Message message) {
        return process(message, (Audience) null);
    }

    @Override
    public Component process(Message message, Audience target) {
        Locale locale = getUserLocale(target);
        return process(message, locale);
    }

    @Override
    public Component process(Message message, Locale locale) {
        String raw = message.getDictionary().get(locale);
        if (raw == null && !"".equals(locale.getVariant())) {
            raw = message.getDictionary().get(new Locale(locale.getLanguage(), locale.getCountry()));
        }
        if (raw == null && !"".equals(locale.getCountry())) {
            raw = message.getDictionary().get(new Locale(locale.getLanguage()));
        }
        if (raw == null) {
            raw = message.getDictionary().get(TinyTranslations.DEFAULT_LOCALE);
        }
        if (raw == null) {
            raw = "<no-translation-found:" + message.getNamespacedKey() + "/>";
        }

        List<TagResolver> resolvers = new ArrayList<>(message.getResolvers());
        if (message.getTranslator() != null) {
            resolvers.add(message.getTranslator().getResolvers(locale));
        }

        return process(raw, locale, resolvers.toArray(TagResolver[]::new));
    }

    @Override
    public Component process(String raw, TagResolver... resolvers) {
        return process(raw, (Audience) null, resolvers);
    }

    @Override
    public Component process(String raw, Audience target, TagResolver... resolvers) {
        return process(raw, getUserLocale(target), resolvers);
    }

    @Override
    public Component process(String raw, Locale locale, TagResolver... resolvers) {
        String processed = preprocessor.apply(raw);
        return MM.deserialize(processed, TagResolver.builder()
                .resolvers(resolvers)
                .resolver(getResolvers(locale))
                .build());
    }

    @Override
    public TagResolver getResolvers(Locale locale) {
        return TagResolver.resolver(getStylesResolver(), getMessageResolver(locale), defaultResolvers);
    }

    private TagResolver getStylesResolver() {
        if (styleResolverCache != null) {
            return styleResolverCache;
        }
        Map<String, TagResolver> styles = new HashMap<>();

        Translator t = this;
        while (t != null) {
            t.getStyleSet().forEach((key, value) -> {
                if (styles.containsKey(key)) return;
                styles.put(key, value.getResolver());
            });
            t = t.getParent();
        }
        styles.put("style", TagResolver.resolver("style", (argumentQueue, context) -> {
            String styleKey = argumentQueue.popOr("A style tag requires a specified style").value();
            if (argumentQueue.hasNext()) {
                String namespace = styleKey;
                styleKey = argumentQueue.pop().value();
                return getStyleByNamespace(namespace, styleKey).getResolver().resolve(styleKey, argumentQueue, context);
            }
            return styles.get(styleKey).resolve(styleKey, argumentQueue, context);
        }));
        styleResolverCache = TagResolver.resolver(styles.values());
        return styleResolverCache;
    }

    private TagResolver getMessageResolver(Locale locale) {
        return TagResolver.resolver("msg", (queue, ctx) -> {
            String nameSpace;
            String key = queue.popOr("The message tag requires a message key, like <msg:error.no_permission/>.").value();
            if (queue.hasNext()) {
                nameSpace = key;
                key = queue.pop().value();
            } else {
                Message msg = getMessageInParentTree(key);
                if (msg == null) {
                    return Tag.inserting(Component.text("<msg-not-found:" + key + "/>"));
                }
                return Tag.inserting(process(msg, locale));
            }
            Message msg = getMessageByNamespace(nameSpace, key);
            if (msg == null) {
                return Tag.inserting(Component.text("<msg-not-found:" + nameSpace + ":" + key + "/>"));
            }
            return Tag.inserting(process(msg, locale));
        });
    }

    @Override
    public @Nullable Message getMessage(String key) {
        return messageSet.get(key);
    }

    public @Nullable MessageStyle getStyle(String key) {
        return styleSet.get(key);
    }

    public @Nullable MessageStyle getStyleInParentTree(String key) {
        MessageStyle style = getStyle(key);
        if (style != null) return style;
        if (parent == null) return null;
        return parent.getStyleInParentTree(key);
    }

    @Override
    public @Nullable Message getMessageInParentTree(String key) {
        Message msg = getMessage(key);
        if (msg != null) return msg;
        if (parent == null) return null;
        return parent.getMessageInParentTree(key);
    }

    private @Nullable Translator getTranslationsByNamespace(@AppPathPattern String namespace) {
        Translator translator = this;
        String[] split = namespace.split("\\.");
        Queue<String> path = new LinkedList<>(List.of(split));

        // remove global from queue
        path.poll();

        while (!path.isEmpty()) {
            String childName = path.poll();
            translator = children.get(childName);
            if (translator == null) {
                return null;
            }
        }
        return translator;
    }

    @Override
    public @Nullable Message getMessageByNamespace(@AppPathPattern String namespace, String key) {
        if (parent != null) {
            return parent.getMessageByNamespace(namespace, key);
        }
        Translator translator = getTranslationsByNamespace(namespace);
        return translator == null ? null : translator.getMessageInParentTree(key);
    }

    @Override
    public @Nullable MessageStyle getStyleByNamespace(@AppPathPattern String namespace, String key) {
        if (parent != null) {
            return parent.getStyleByNamespace(namespace, key);
        }
        Translator translator = getTranslationsByNamespace(namespace);
        return translator == null ? null : translator.getStyleInParentTree(key);
    }

    @Override
    public void addMessage(Message message) {
        if (message.getTranslator() != null) {
            message.getTranslator().getMessageSet().remove(message.getKey());
        }
        messageSet.put(message.getKey(), message);
    }

    @Override
    public void addMessages(Message... messages) {
        for (Message message : messages) {
            addMessage(message);
        }
    }

    @Override
    public void addMessage(Iterable<Message> messages) {
        for (Message message : messages) {
            addMessage(message);
        }
    }

    @Override
    public void loadStyles() {
        try {
            if (parent != null) {
                parent.loadStyles();
            }
            if (styleStorage != null) {
                styleSet.putAll(styleStorage.loadStyles());
                styleResolverCache = null;
                getStylesResolver(); // create cache in loading process where performance leaks are expected
            }
        } catch (Throwable t) {

        }
    }

    @Override
    public void saveStyles() {
        if (styleStorage != null) {
            styleStorage.writeStyles(styleSet);
        }
    }

    @Override
    public void loadLocales() {
        for (Locale availableLocale : Locale.getAvailableLocales()) {
            loadLocale(availableLocale);
        }
    }

    @Override
    public void loadLocale(Locale locale) {
        if (parent != null) {
            parent.loadLocale(locale);
        }
        if (messageStorage != null) {
            messageStorage.readMessages(locale).forEach((message, s) -> {
                messageSet.computeIfAbsent(message.getKey(), key -> message).getDictionary().put(locale, s);
            });
        }
    }

    @Override
    public void saveLocale(Locale locale) {
        if (messageStorage != null) {
            messageStorage.writeMessages(messageSet.values(), locale);
        }
    }

    @Override
    public void setLocaleProvider(Function<@Nullable Audience, @NotNull Locale> function) {
        localeProvider = function;
    }

    @Override
    public @NotNull Locale getUserLocale(@Nullable Audience user) {
        if (localeProvider != null) {
            return localeProvider.apply(user);
        }
        return parent == null ? Locale.ENGLISH : parent.getUserLocale(user);
    }
}