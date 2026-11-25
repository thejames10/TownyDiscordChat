package com.TownyDiscordChat.TownyDiscordChat.Updates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** One release from the GitHub releases API. */
public final class PublishedRelease {
    private static final String TAG = "tag_name";
    private static final String PAGE = "html_url";
    private static final String DRAFT = "draft";
    private static final String PRE_RELEASE = "prerelease";

    private final ReleaseVersion version;
    private final String url;

    PublishedRelease(ReleaseVersion version, String url) {
        this.version = version;
        this.url = url;
    }

    public ReleaseVersion version() {
        return version;
    }

    public String url() {
        return url;
    }

    /** The highest published version in a GitHub releases response, or null. */
    @SuppressWarnings("deprecation")
    public static PublishedRelease newest(String releasesJson, boolean includePreReleases) {
        // The instance parse method exists in every Gson that servers bundle;
        // the static replacement is missing from the Gson in 1.16.5.
        JsonElement parsed = new JsonParser().parse(releasesJson);
        if (!parsed.isJsonArray()) {
            return null;
        }
        PublishedRelease newest = null;
        JsonArray releases = parsed.getAsJsonArray();
        for (JsonElement element : releases) {
            PublishedRelease candidate = eligible(element, includePreReleases);
            if (candidate != null && (newest == null || candidate.version.isNewerThan(newest.version))) {
                newest = candidate;
            }
        }
        return newest;
    }

    private static PublishedRelease eligible(JsonElement element, boolean includePreReleases) {
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject release = element.getAsJsonObject();
        if (flag(release, DRAFT) || (!includePreReleases && flag(release, PRE_RELEASE))
                || !release.has(TAG) || !release.has(PAGE)) {
            return null;
        }
        ReleaseVersion version = ReleaseVersion.parse(release.get(TAG).getAsString());
        return version == null ? null : new PublishedRelease(version, release.get(PAGE).getAsString());
    }

    private static boolean flag(JsonObject release, String name) {
        return release.has(name) && release.get(name).getAsBoolean();
    }
}
