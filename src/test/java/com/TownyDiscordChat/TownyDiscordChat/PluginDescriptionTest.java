package com.TownyDiscordChat.TownyDiscordChat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** plugin.yml is read as text: Bukkit's YAML loader would split nodes at each period. */
public class PluginDescriptionTest {
    private static final String PERMISSION_INDENT = "\n  ";
    private static final String[] LEGACY_NODES = {
            "townydiscordchat.check.role",
            "townydiscordchat.check.role.alllinked",
            "townydiscordchat.check.role.createalltownsandnations",
            "townydiscordchat.check.textchannel.alltownsandnations",
            "townydiscordchat.check.voicechannel.alltownsandnations"};

    private static String pluginDescription() throws IOException {
        return new String(Files.readAllBytes(Paths.get("src/main/resources/plugin.yml")),
                StandardCharsets.UTF_8);
    }

    private static String block(String text, String node) {
        int start = text.indexOf(PERMISSION_INDENT + node + ":\n");
        assertTrue(start >= 0, node + " is missing from plugin.yml");
        int end = text.indexOf(PERMISSION_INDENT + "townydiscordchat.", start + 1);
        return text.substring(start, end < 0 ? text.length() : end);
    }

    @Test
    public void everyPermissionConstantIsDeclared() throws IOException, IllegalAccessException {
        String text = pluginDescription();
        for (Field field : Permissions.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                block(text, (String) field.get(null));
            }
        }
    }

    @Test
    public void adminGetsEveryCommand() throws IOException {
        String admin = block(pluginDescription(), Permissions.ADMIN);
        for (String node : new String[]{Permissions.PLAYER, Permissions.RELOAD, Permissions.DEBUG,
                Permissions.QUEUE, Permissions.RECONCILE, Permissions.NOTIFY}) {
            assertTrue(admin.contains(node + ": true"), "admin must include " + node);
        }
    }

    @Test
    public void legacyCheckNodesStillGrantReconcile() throws IOException {
        String text = pluginDescription();
        for (String node : LEGACY_NODES) {
            assertTrue(block(text, node).contains(Permissions.RECONCILE + ": true"), node);
        }
    }
}
