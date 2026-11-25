package com.TownyDiscordChat.TownyDiscordChat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DependencyIsolationTest {
    private static final Path MAIN_SOURCES = Paths.get("src/main/java");
    private static final Path MAIN_CLASS =
            Paths.get("src/main/java/com/TownyDiscordChat/TownyDiscordChat/Main.java");
    // DiscordSRV relocates its own libraries under this package. Only its JDA
    // API is a supported integration point; the rest disappears with DiscordSRV.
    private static final Pattern BUNDLED_LIBRARY_IMPORT = Pattern.compile(
            "import\\s+github\\.scarsz\\.discordsrv\\.dependencies\\.(?!jda\\.)");

    @Test
    public void pluginNeverUsesDiscordSrvBundledLibraries() throws IOException {
        List<Path> offenders;
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            offenders = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(DependencyIsolationTest::importsBundledLibrary)
                    .collect(Collectors.toList());
        }

        assertTrue(offenders.isEmpty(), "Uses DiscordSRV's bundled libraries: " + offenders);
    }

    @Test
    public void disabledDependencyStopsEverySynchronizationComponent() throws IOException {
        String source = read(MAIN_CLASS);
        int start = source.indexOf("private void stopSynchronizationServices()");
        int end = source.indexOf("private static void unregisterListener(", start);

        assertTrue(start >= 0 && end > start, "stopSynchronizationServices is missing");
        String body = source.substring(start, end);
        assertTrue(body.contains("operational = false"));
        assertTrue(body.contains("unsubscribeAccountListener()"));
        assertTrue(body.contains("unregisterListener(membershipListener)"));
        assertTrue(body.contains("unregisterListener(onboarding)"));
        assertTrue(body.contains("getServer().getScheduler().cancelTasks(this)"));
    }

    private static boolean importsBundledLibrary(Path path) {
        try {
            return BUNDLED_LIBRARY_IMPORT.matcher(read(path)).find();
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read " + path, error);
        }
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
