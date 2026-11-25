package com.TownyDiscordChat.TownyDiscordChat.Updates;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The numeric part of a version such as 1.2.0, v1.2.0 or 1.2.0-SNAPSHOT. */
public final class ReleaseVersion implements Comparable<ReleaseVersion> {
    private static final Pattern NUMBERS = Pattern.compile("^[vV]?(\\d+(?:\\.\\d+)*)");
    private static final String SEPARATOR = "\\.";

    private final int[] parts;
    private final String text;

    private ReleaseVersion(int[] parts, String text) {
        this.parts = parts;
        this.text = text;
    }

    /** Null when the text does not start with a version number. */
    public static ReleaseVersion parse(String version) {
        if (version == null) {
            return null;
        }
        Matcher matcher = NUMBERS.matcher(version.trim());
        if (!matcher.find()) {
            return null;
        }
        String numbers = matcher.group(1);
        try {
            return new ReleaseVersion(Arrays.stream(numbers.split(SEPARATOR))
                    .mapToInt(Integer::parseInt).toArray(), numbers);
        } catch (NumberFormatException tooLarge) {
            return null;
        }
    }

    public boolean isNewerThan(ReleaseVersion other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(ReleaseVersion other) {
        int length = Math.max(parts.length, other.parts.length);
        for (int i = 0; i < length; i++) {
            int difference = Integer.compare(part(i), other.part(i));
            if (difference != 0) {
                return difference;
            }
        }
        return 0;
    }

    private int part(int index) {
        return index < parts.length ? parts[index] : 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReleaseVersion && compareTo((ReleaseVersion) other) == 0;
    }

    @Override
    public int hashCode() {
        int length = parts.length;
        while (length > 0 && parts[length - 1] == 0) {
            length--;
        }
        return Arrays.hashCode(Arrays.copyOf(parts, length));
    }

    @Override
    public String toString() {
        return text;
    }
}
