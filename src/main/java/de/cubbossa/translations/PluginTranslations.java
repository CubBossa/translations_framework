package de.cubbossa.translations;

import de.cubbossa.translations.serialize.StorageType;
import lombok.experimental.Accessors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public interface PluginTranslations extends Translator {

    @Accessors(fluent = true, chain = true)
    class Config {
        protected Locale defaultLanguage = Locale.US;
        protected boolean preferClientLanguage = false;
        protected StorageType languageFileStorageType = StorageType.PROPERTIES;
        protected StorageType styleFileStorageType = StorageType.PROPERTIES;
    }

    CompletableFuture<Void> writeLocale(Locale locale);

    CompletableFuture<Void> cacheLocale(Locale locale);

    void addMessage(Message message);

    void addMessages(Message...messages);

    void addMessagesClass(Class<?> fromClass);

    Config getConfig();

    default Locale getLanguage(@Nullable Audience audience) {
        if (audience == null) {
            return getConfig().defaultLanguage;
        }
        if (getConfig().preferClientLanguage) {
            return audience.getOrDefault(Identity.LOCALE, Locale.US);
        }
        return getConfig().defaultLanguage;
    }

    Collection<TagResolver> getGlobalResolvers();

    void addGlobalResolver(TagResolver resolver);
}
