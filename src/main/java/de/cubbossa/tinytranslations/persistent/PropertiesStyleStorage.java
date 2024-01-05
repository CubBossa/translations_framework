package de.cubbossa.tinytranslations.persistent;

import de.cubbossa.tinytranslations.MessageStyle;
import de.cubbossa.tinytranslations.MessageStyleImpl;
import de.cubbossa.tinytranslations.StyleDeserializer;
import de.cubbossa.tinytranslations.StyleDeserializerImpl;
import de.cubbossa.tinytranslations.util.Entry;
import de.cubbossa.tinytranslations.util.PropertiesUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class PropertiesStyleStorage implements StyleStorage {

    private final File file;
    private final StyleDeserializer styleDeserializer;

    public PropertiesStyleStorage(File file) {
        this(file, new StyleDeserializerImpl());
    }

    public PropertiesStyleStorage(File file, StyleDeserializer styleDeserializer) {
        this.file = file;
        this.styleDeserializer = styleDeserializer;
    }

    private File file() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (Throwable t) {
                throw new IllegalStateException("Could not create properties file.", t);
            }
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("PropertiesStyles requires a properties file as argument");
        }
        if (!file.getName().endsWith(".properties")) {
            throw new IllegalArgumentException("PropertiesStyles requires a properties file as argument");
        }
        return file;
    }

    @Override
    public void writeStyles(Map<String, MessageStyle> styles) {
        List<Entry> lines = readStylesFile(file());
        Map<String, MessageStyle> present = readStylesFromLines(lines);
        Map<String, MessageStyle> toWrite = new LinkedHashMap<>();

        for (var stylePair : styles.entrySet()) {
            if (!present.containsKey(stylePair.getKey())) {
                toWrite.put(stylePair.getKey(), stylePair.getValue());
            }
        }

        toWrite.forEach((s, style) -> lines.add(new Entry(s, style.getStringBackup(), style instanceof FileEntry e ? e.getComments() : Collections.emptyList())));
        writeStyles(lines);
    }

    private void writeStyles(List<Entry> lines) {
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            PropertiesUtils.write(writer, lines);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, MessageStyle> loadStyles() {
        return readStylesFromLines(readStylesFile(file()));
    }

    private Map<String, MessageStyle> readStylesFromLines(List<Entry> lines) {
        return lines.stream().collect(Collectors.toMap(
                Entry::key,
                e -> new MessageStyleImpl(e.key(), styleDeserializer.deserialize(e.key(), e.value()), e.value()),
                (a, b) -> a,
                LinkedHashMap::new
        ));
    }

    private List<Entry> readStylesFile(File file) {
        try (Reader r = new FileReader(file, StandardCharsets.UTF_8)) {
            return PropertiesUtils.loadProperties(r);
        } catch (Throwable t) {
            throw new RuntimeException("Error while parsing locale file '" + file.getAbsolutePath() + "'.", t);
        }
    }

    private interface Line {
        String print();
    }
}