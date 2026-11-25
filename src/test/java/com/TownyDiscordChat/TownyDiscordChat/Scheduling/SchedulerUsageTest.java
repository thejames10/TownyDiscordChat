package com.TownyDiscordChat.TownyDiscordChat.Scheduling;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulerUsageTest {
    private static final Path MAIN_SOURCES = Paths.get("src/main/java");
    private static final Path SCHEDULING_PACKAGE =
            Paths.get("src/main/java/com/TownyDiscordChat/TownyDiscordChat/Scheduling");
    private static final Path PLUGIN_DESCRIPTION = Paths.get("src/main/resources/plugin.yml");
    // Folia throws UnsupportedOperationException from the Bukkit scheduler.
    private static final Pattern BUKKIT_SCHEDULER_USE =
            Pattern.compile("getScheduler\\(\\)|BukkitTask|BukkitRunnable");

    @Test
    public void onlyTheSchedulingPackageTouchesTheBukkitScheduler() throws IOException {
        List<Path> offenders;
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            offenders = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(SCHEDULING_PACKAGE))
                    .filter(SchedulerUsageTest::usesBukkitScheduler)
                    .collect(Collectors.toList());
        }

        assertTrue(offenders.isEmpty(), "Schedule through TaskScheduler instead: " + offenders);
    }

    @Test
    public void pluginDeclaresFoliaSupport() throws IOException {
        assertTrue(read(PLUGIN_DESCRIPTION).contains("folia-supported: true"));
    }

    @Test
    public void bukkitServersAreNotDetectedAsFolia() {
        assertFalse(TaskSchedulers.isFolia());
    }

    private static boolean usesBukkitScheduler(Path path) {
        try {
            return BUKKIT_SCHEDULER_USE.matcher(read(path)).find();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
