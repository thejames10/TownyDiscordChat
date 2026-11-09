package com.TownyDiscordChat.TownyDiscordChat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompatibilityGuardTest {
    @Test
    public void reportsBothMissingDependencies() {
        String problem = CompatibilityGuard.missingDependencyProblem(false, false);

        assertTrue(problem.contains("DiscordSRV missing or disabled"));
        assertTrue(problem.contains("Towny missing or disabled"));
        assertTrue(problem.contains("Towny 0.97.5.0+"));
        assertTrue(problem.contains("DiscordSRV 1.24.0+"));
    }

    @Test
    public void reportsOnlyMissingDiscordSrv() {
        String problem = CompatibilityGuard.missingDependencyProblem(false, true);

        assertTrue(problem.contains("DiscordSRV missing or disabled"));
        assertFalse(problem.contains("Towny missing or disabled"));
    }

    @Test
    public void reportsOnlyMissingTowny() {
        String problem = CompatibilityGuard.missingDependencyProblem(true, false);

        assertTrue(problem.contains("Towny missing or disabled"));
        assertTrue(problem.contains("Towny 0.97.5.0+"));
        assertFalse(problem.contains("DiscordSRV missing or disabled"));
    }

    @Test
    public void doesNotReportMissingDependenciesWhenBothAreReady() {
        assertNull(CompatibilityGuard.missingDependencyProblem(true, true));
    }
}
