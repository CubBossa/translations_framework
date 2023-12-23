package de.cubbossa.translations;

import java.util.LinkedList;
import java.util.List;

public enum NamedGlobalStyles {

	PRIMARY("primary"),
	PRIMARY_LIGHT("primary_l", "primary_light"),
	PRIMARY_DARK("primary_d", "primary_dark"),
	OFFSET("offset"),
	OFFSET_LIGHT("offset_l", "offset_light"),
	OFFSET_DARK("offset_d", "offset_dark"),
	ACCENT("accent"),
	ACCENT_LIGHT("accent_l", "accent_light"),
	ACCENT_DARK("accent_d", "accent_dark"),

	NOTIFY("notify"),
	INFO("info"),
	NEGATIVE("negative"),
	POSITIVE("positive"),
	WARNING("warning"),

	TEXT("text"),
	TEXT_LIGHT("text_l", "text_light"),
	TEXT_DARK("text_d", "text_dark"),
	TEXT_HIGHLIGHT("text_hl", "text_highlight"),

	BACKGROUND("bg", "background"),
	BACKGROUND_LIGHT("bg_l", "background_light"),
	BACKGROUND_DARK("bg_d", "background_dark"),

	PREFIX("prefix"),
	PREFIXED("prefixed"),

	SEPARATOR("separator"),
	BUTTON("button"),

	LIST_ELEMENT("list_el", "list_element"),
	PREVIOUS_PAGE("prev_page_symbol"),
	NEXT_PAGE("next_page_symbol"),

	URL("url"),

	CMD_SYNTAX("cmd_syntax"),
	CMD_ARGUMENT("arg"),
	CMD_ARGUMENT_OPTIONAL("arg_opt", "arg_0_1"),
	CMD_ARGUMENT_0_TO_MANY("arg_0_n"),
	CMD_ARGUMENT_1_TO_MANY("arg_1_n"),
	;

	private final List<String> keys = new LinkedList<>();

	NamedGlobalStyles(String key, String... alias) {
		keys.add(key);
		keys.addAll(List.of(alias));
	}

	private String key() {
		return keys.get(0);
	}

	@Override
	public String toString() {
		return key();
	}
}
