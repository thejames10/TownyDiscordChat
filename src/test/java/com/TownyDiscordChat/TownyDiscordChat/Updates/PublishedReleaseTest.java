package com.TownyDiscordChat.TownyDiscordChat.Updates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PublishedReleaseTest {
    private static final String RELEASES = "["
            + release("v1.3.0", true, false)
            + "," + release("v1.2.1", false, false)
            + "," + release("v1.4.0", false, true)
            + "," + release("nightly", false, false)
            + "]";

    private static String release(String tag, boolean preRelease, boolean draft) {
        return "{\"tag_name\":\"" + tag + "\",\"html_url\":\"https://example.invalid/" + tag
                + "\",\"prerelease\":" + preRelease + ",\"draft\":" + draft + "}";
    }

    @Test
    public void stableReleasesOnlyByDefault() {
        PublishedRelease newest = PublishedRelease.newest(RELEASES, false);
        assertEquals("1.2.1", newest.version().toString());
        assertEquals("https://example.invalid/v1.2.1", newest.url());
    }

    @Test
    public void preReleasesWhenAskedButNeverDrafts() {
        assertEquals("1.3.0", PublishedRelease.newest(RELEASES, true).version().toString());
    }

    @Test
    public void unexpectedResponsesFindNothing() {
        assertNull(PublishedRelease.newest("{\"message\":\"rate limited\"}", true));
        assertNull(PublishedRelease.newest("[]", true));
    }
}
