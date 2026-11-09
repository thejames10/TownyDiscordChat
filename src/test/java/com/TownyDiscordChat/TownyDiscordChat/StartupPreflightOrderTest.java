package com.TownyDiscordChat.TownyDiscordChat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StartupPreflightOrderTest {
    private static final String MAIN_SOURCE =
            "src/main/java/com/TownyDiscordChat/TownyDiscordChat/Main.java";

    @Test
    public void dependencyPreflightRunsBeforeAnyConfigurationWork() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(MAIN_SOURCE)),
                StandardCharsets.UTF_8);
        int enableStart = source.indexOf("public void onEnable() {");
        int disableStart = source.indexOf("public void onDisable()");

        assertTrue(enableStart >= 0 && disableStart > enableStart);
        String startup = source.substring(enableStart, disableStart);
        int preflight = startup.indexOf("compatibility.check()");

        assertTrue(preflight >= 0);
        assertBefore(startup, preflight, "ConfigUpgrade.migrateIfNeeded(");
        assertBefore(startup, preflight, "saveDefaultConfig()");
        assertBefore(startup, preflight, "SetupValidator.validateLocal(");
        assertBefore(startup, preflight, "new Database(");
        assertFalse(source.contains("FileConfiguration config = getConfig()"),
                "Do not eagerly read config during plugin construction");
    }

    private static void assertBefore(String source, int earlier, String laterText) {
        int later = source.indexOf(laterText);
        assertTrue(later > earlier, laterText + " must occur after compatibility.check()");
    }
}
