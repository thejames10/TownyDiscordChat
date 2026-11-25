package com.TownyDiscordChat.TownyDiscordChat.Messages;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-game text from messages.yml. Keys missing from the server's file fall back
 * to the bundled English text, so messages added by updates always resolve.
 */
public final class MessageCatalog {
    public static final String FILE_NAME = "messages.yml";
    private static final String PREFIX_KEY = "prefix";
    private static final char COLOR_CODE = '&';
    private static final String SECTION_SIGN = "§";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z]+)}");

    private final ConfigurationSection messages;

    private MessageCatalog(ConfigurationSection messages) {
        this.messages = messages;
    }

    /**
     * Creates messages.yml on first use, carrying over text customised in a
     * 1.1.x config.yml, then loads it over the bundled defaults.
     */
    public static MessageCatalog load(JavaPlugin plugin, ConfigurationSection legacyConfig)
            throws IOException, InvalidConfigurationException {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
            applyOverrides(file, LegacyMessages.overrides(legacyConfig));
        }
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.load(file);
        loaded.setDefaults(bundled(plugin));
        return new MessageCatalog(loaded);
    }

    /** Built-in English text, for before messages.yml can be read. */
    public static MessageCatalog builtIn(JavaPlugin plugin) throws IOException {
        return new MessageCatalog(bundled(plugin));
    }

    static MessageCatalog of(ConfigurationSection messages) {
        return new MessageCatalog(messages);
    }

    private static YamlConfiguration bundled(JavaPlugin plugin) throws IOException {
        InputStream resource = plugin.getResource(FILE_NAME);
        if (resource == null) {
            throw new IOException("The plugin JAR is missing " + FILE_NAME);
        }
        try (Reader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    private static void applyOverrides(File file, Map<String, String> overrides)
            throws IOException, InvalidConfigurationException {
        if (overrides.isEmpty()) {
            return;
        }
        YamlConfiguration created = new YamlConfiguration();
        created.load(file);
        for (Map.Entry<String, String> override : overrides.entrySet()) {
            created.set(override.getKey(), override.getValue());
        }
        created.save(file);
    }

    /** Sends each line of a message; a message set to "" is not sent at all. */
    public void send(CommandSender recipient, String key, String... placeholders) {
        for (String line : lines(key, placeholders)) {
            if (!line.isEmpty()) {
                recipient.sendMessage(line);
            }
        }
    }

    public String text(String key, String... placeholders) {
        return String.join("\n", lines(key, placeholders));
    }

    public List<String> lines(String key, String... placeholders) {
        Map<String, String> values = pairs(placeholders);
        values.put(PREFIX_KEY, color(stringOrKey(PREFIX_KEY)));
        List<String> lines = new ArrayList<>();
        for (String template : templates(key)) {
            lines.add(fill(color(template), values));
        }
        return lines;
    }

    private List<String> templates(String key) {
        if (messages.isList(key)) {
            return messages.getStringList(key);
        }
        return Collections.singletonList(stringOrKey(key));
    }

    // Showing the key makes a missing message visible instead of silent.
    private String stringOrKey(String key) {
        String value = messages.getString(key);
        return value == null ? key : value;
    }

    private static Map<String, String> pairs(String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be name/value pairs");
        }
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < placeholders.length; i += 2) {
            values.put(placeholders[i], plain(placeholders[i + 1]));
        }
        return values;
    }

    /** Fills placeholders in one pass, so a value can never introduce another placeholder. */
    static String fill(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer filled = new StringBuffer();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            String replacement = value == null ? matcher.group() : value;
            matcher.appendReplacement(filled, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(filled);
        return filled.toString();
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes(COLOR_CODE, text);
    }

    // Player names and Discord text must not carry their own colour codes.
    private static String plain(String value) {
        return value == null ? "" : value.replace(SECTION_SIGN, "");
    }
}
