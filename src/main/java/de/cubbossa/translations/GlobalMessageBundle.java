package de.cubbossa.translations;

import de.cubbossa.translations.persistent.PropertiesStorage;
import de.cubbossa.translations.persistent.PropertiesStyles;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

@Getter
@Setter
public class GlobalMessageBundle extends AbstractMessageBundle implements MessageBundle {

    private static GlobalMessageBundle instance;

    public static GlobalMessageBundle get() {
        return instance;
    }

    public static PluginTranslationsBuilder applicationTranslationsBuilder(String pluginName, File pluginDir) {
        GlobalMessageBundle translations = GlobalMessageBundle.get();
        if (translations == null) {
            translations = new GlobalMessageBundle();
        }
        if (translations.config == null) {
            translations.config = new Config()
                    .localeBundleStorage(new PropertiesStorage(Logger.getLogger("Translations"), new File(pluginDir, "../Translations")))
                    .stylesStorage(new PropertiesStyles(new File(pluginDir, "../Translations/global_styles.properties")));
        }
        return new PluginTranslationsBuilder(translations, pluginName);
    }

    public synchronized void register(String name, MessageBundle translations) {
        GlobalMessageBundle t = GlobalMessageBundle.get();
        if (t == null) {
            t = new GlobalMessageBundle();
        }

        if (t.applicationMap.containsKey(name)) {
            throw new IllegalArgumentException("Could not register new PluginTranslations, another translation with" +
                    "key '" + name + "' already exists");
        }
        t.applicationMap.put(name, translations);


    }

    private final Map<String, MessageBundle> applicationMap;
    private final Collection<TagResolver> bundleResolvers;

    public GlobalMessageBundle() {
        super(null, Logger.getLogger("Translations"));
        instance = this;

        this.applicationMap = new HashMap<>();
        this.bundleResolvers = new ArrayList<>();
    }

    @Override
    protected Locale supportedLocale(Locale anyLocale) {
        return translationCache.containsKey(anyLocale) ? anyLocale : config.defaultLocale;
    }

    @Override
    public TagResolver getBundleResolvers() {
        return TagResolver.resolver(bundleResolvers);
    }

    private Message getAppOrGlobalMessage(String key) {
        return getApplicationFromKey(key)
                .map(bundle -> bundle.getMessage(key.substring(key.indexOf('.') + 1)))
                .orElseGet(() -> getMessage(key));
    }

    private Optional<MessageBundle> getApplicationFromKey(String messageKey) {
        return Optional.ofNullable(applicationMap.get(messageKey.split("\\.")[0]));
    }

    private Optional<MessageBundle> getApplicationFromKey(Message message) {
        return getApplicationFromKey(message.getKey());
    }

    @Override
    public TagResolver getMessageResolver(Audience audience) {
        // TODO proper loop detection
        return TagResolver.resolver("gmsg", (queue, ctx) -> {
            String messageKey = queue.popOr("The message tag requires a message key, like <gmsg:myplugin.error.no_permission>.").value();
            boolean preventBleed = queue.hasNext() && queue.pop().isTrue();

            Message msg = getAppOrGlobalMessage(messageKey);
            return preventBleed
                    ? Tag.selfClosingInserting(msg.getTranslator().translate(msg, audience))
                    : Tag.preProcessParsed(msg.getTranslator().translateRaw(msg, audience));
        });
    }

    @Override
    public String translateRaw(Message message) {
        return getApplicationFromKey(message).orElseThrow().translateRaw(message);
    }

    @Override
    public String translateRaw(Message message, Audience audience) {
        return getApplicationFromKey(message).orElseThrow().translateRaw(message, audience);
    }

    @Override
    public String translateRaw(Message message, Locale locale) {
        return getApplicationFromKey(message).orElseThrow().translateRaw(message, locale);
    }

    @Override
    public Component translate(Message message) {
        return getApplicationFromKey(message).orElseThrow().translate(message);
    }

    @Override
    public Component translate(Message message, Audience audience) {
        return getApplicationFromKey(message).orElseThrow().translate(message, audience);
    }

    @Override
    public Component translate(Message message, Locale locale) {
        return getApplicationFromKey(message).orElseThrow().translate(message, locale);
    }
}