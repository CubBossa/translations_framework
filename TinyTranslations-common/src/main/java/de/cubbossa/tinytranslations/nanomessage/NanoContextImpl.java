package de.cubbossa.tinytranslations.nanomessage;

import de.cubbossa.tinytranslations.Message;
import de.cubbossa.tinytranslations.nanomessage.tag.NanoResolver;
import de.cubbossa.tinytranslations.util.MessageUtil;
import de.cubbossa.tinytranslations.TinyTranslations;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.intellij.lang.annotations.Language;

import java.util.*;

@Getter
public class NanoContextImpl {
	private final Locale locale;
	private final List<NanoResolver> resolvers;

	public NanoContextImpl(Locale locale) {
		this.locale = locale;
		this.resolvers = Collections.emptyList();
	}

	public NanoContextImpl(Locale locale, NanoResolver... resolvers) {
		this.locale = locale;
		this.resolvers = List.of(resolvers);
	}

	public NanoContextImpl(Locale locale, TagResolver... resolvers) {
		this.locale = locale;
		this.resolvers = Arrays.stream(resolvers).map(r -> ((NanoResolver) c -> r)).toList();
	}

	public NanoContextImpl(Locale locale, Collection<NanoResolver> resolvers) {
		this.locale = locale;
		this.resolvers = List.copyOf(resolvers);
	}

	public Component process(@Language("NanoMessage") String raw, TagResolver... resolvers) {
		return TinyTranslations.NM.deserialize(raw, this, resolvers);
	}

	public Component process(Message message, TagResolver... resolvers) {
		return process(MessageUtil.getMessageTranslation(message, locale), resolvers);
	}

	public ObjectTagResolverMap getObjectTagResolverMap() {
		return TinyTranslations.NM.getObjectTypeResolverMap();
	}
}