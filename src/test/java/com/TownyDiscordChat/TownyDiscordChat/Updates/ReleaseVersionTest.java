package com.TownyDiscordChat.TownyDiscordChat.Updates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReleaseVersionTest {
    @Test
    public void tagsAndQualifiersReduceToTheirNumbers() {
        assertEquals("1.2.0", ReleaseVersion.parse("v1.2.0").toString());
        assertEquals("1.2.0", ReleaseVersion.parse("1.2.0-SNAPSHOT").toString());
    }

    @Test
    public void numbersCompareNumericallyNotAlphabetically() {
        assertTrue(ReleaseVersion.parse("1.10.0").isNewerThan(ReleaseVersion.parse("1.9.9")));
        assertTrue(ReleaseVersion.parse("1.2").isNewerThan(ReleaseVersion.parse("1.1.9")));
        assertFalse(ReleaseVersion.parse("1.1.2").isNewerThan(ReleaseVersion.parse("1.1.2")));
    }

    @Test
    public void trailingZeroesAreEqual() {
        assertEquals(ReleaseVersion.parse("1.2"), ReleaseVersion.parse("1.2.0"));
        assertEquals(ReleaseVersion.parse("1.2").hashCode(), ReleaseVersion.parse("1.2.0").hashCode());
    }

    @Test
    public void textWithoutAVersionIsRejected() {
        assertNull(ReleaseVersion.parse("latest"));
        assertNull(ReleaseVersion.parse(null));
        assertNull(ReleaseVersion.parse("99999999999.0"));
    }
}
