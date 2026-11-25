package com.TownyDiscordChat.TownyDiscordChat.Messages;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageCatalogTest {
    private static final Path MAIN_SOURCES = Paths.get("src/main/java");
    private static final Pattern MESSAGE_KEY_USE =
            Pattern.compile("(?:(?:send|reply)\\([^,()]+,\\s*|text\\(|lines\\()\"([a-z][a-z.-]*[a-z])\"");
    private static final String SECTION_SIGN = "§";

    private static YamlConfiguration bundled() throws IOException {
        try (InputStream stream = MessageCatalogTest.class.getClassLoader()
                .getResourceAsStream(MessageCatalog.FILE_NAME)) {
            assertNotNull(stream, "messages.yml must be bundled");
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        }
    }

    @Test
    public void everyKeyTheCodeSendsExistsInTheBundledFile() throws IOException {
        YamlConfiguration messages = bundled();
        Set<String> missing = new LinkedHashSet<>();
        for (String key : keysUsedInCode()) {
            if (!messages.contains(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "Missing from messages.yml: " + missing);
    }

    private static Set<String> keysUsedInCode() throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList())) {
                Matcher matcher = MESSAGE_KEY_USE.matcher(new String(Files.readAllBytes(file),
                        StandardCharsets.UTF_8));
                while (matcher.find()) {
                    keys.add(matcher.group(1));
                }
            }
        }
        assertFalse(keys.isEmpty(), "The key pattern no longer matches any message use");
        return keys;
    }

    @Test
    public void prefixAndPlaceholdersAreFilledAndColoured() throws IOException {
        MessageCatalog catalog = MessageCatalog.of(bundled());
        String line = catalog.text("status.town", "town", "Oakvale");
        assertTrue(line.contains("Oakvale"));
        assertTrue(line.startsWith(SECTION_SIGN), "Colour codes must be translated");
        assertFalse(line.contains("{prefix}"));
    }

    @Test
    public void valuesCannotAddColoursOrPlaceholders() {
        String filled = MessageCatalog.fill("{name} said {message}",
                values("name", "Steve", "message", "{name}"));
        assertEquals("Steve said {name}", filled);
    }

    @Test
    public void unknownPlaceholdersStayVisible() {
        assertEquals("Hello {who}", MessageCatalog.fill("Hello {who}", Collections.emptyMap()));
    }

    @Test
    public void colourCodesInPlayerTextAreRemoved() throws IOException {
        MessageCatalog catalog = MessageCatalog.of(bundled());
        String line = catalog.text("status.town", "town", SECTION_SIGN + "cRedTown");
        assertTrue(line.contains("cRedTown"));
        assertFalse(line.contains(SECTION_SIGN + "cRedTown"));
    }

    @Test
    public void missingKeysShowTheKey() throws IOException {
        assertEquals("no.such.message", MessageCatalog.of(bundled()).text("no.such.message"));
    }

    @Test
    public void customisedLegacyTextIsCarriedOver() {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("messages.Prefix", "&5[Towns]");
        legacy.set("messages.Commands.NoPermission", "&cNope");
        legacy.set("messages.Commands.PleaseWait", "&7Please wait...");
        Map<String, String> overrides = LegacyMessages.overrides(legacy);
        assertEquals("&5[Towns]", overrides.get("prefix"));
        assertEquals("{prefix} &cNope", overrides.get("no-permission"));
        assertFalse(overrides.containsKey("check.requested"), "Unchanged old defaults are dropped");
    }

    private static Map<String, String> values(String... pairs) {
        Map<String, String> values = new java.util.HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return values;
    }
}
