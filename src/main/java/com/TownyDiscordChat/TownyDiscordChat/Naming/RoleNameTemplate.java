package com.TownyDiscordChat.TownyDiscordChat.Naming;

/**
 * A Discord role name with one {name} placeholder for the Towny name, such as
 * "town-{name}". The fixed text around it identifies roles this plugin manages.
 */
public final class RoleNameTemplate {
    public static final String PLACEHOLDER = "{name}";
    // Discord allows 100 characters; Towny names need most of what remains.
    static final int MAXIMUM_FIXED_LENGTH = 50;

    private final String prefix;
    private final String suffix;

    private RoleNameTemplate(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /** @throws IllegalArgumentException with an admin-readable reason */
    public static RoleNameTemplate parse(String template) {
        if (template == null) {
            throw new IllegalArgumentException("is missing");
        }
        int placeholder = template.indexOf(PLACEHOLDER);
        if (placeholder < 0 || template.indexOf(PLACEHOLDER, placeholder + 1) >= 0) {
            throw new IllegalArgumentException("must contain " + PLACEHOLDER + " exactly once");
        }
        String prefix = template.substring(0, placeholder);
        String suffix = template.substring(placeholder + PLACEHOLDER.length());
        if (prefix.trim().isEmpty() && suffix.trim().isEmpty()) {
            throw new IllegalArgumentException("needs text around " + PLACEHOLDER
                    + ", or every role named like a town would be treated as managed");
        }
        if (prefix.length() + suffix.length() > MAXIMUM_FIXED_LENGTH) {
            throw new IllegalArgumentException("may have at most " + MAXIMUM_FIXED_LENGTH
                    + " characters besides " + PLACEHOLDER);
        }
        return new RoleNameTemplate(prefix, suffix);
    }

    public String format(String townyName) {
        return prefix + townyName + suffix;
    }

    /** The Towny name a role name was made from, or null if it does not fit. */
    public String townyName(String roleName) {
        if (roleName == null || roleName.length() <= prefix.length() + suffix.length()
                || !roleName.startsWith(prefix) || !roleName.endsWith(suffix)) {
            return null;
        }
        return roleName.substring(prefix.length(), roleName.length() - suffix.length());
    }

    public boolean matches(String roleName) {
        return townyName(roleName) != null;
    }

    /** True when one role name could fit both templates. */
    boolean overlaps(RoleNameTemplate other) {
        boolean prefixesNest = prefix.startsWith(other.prefix) || other.prefix.startsWith(prefix);
        boolean suffixesNest = suffix.endsWith(other.suffix) || other.suffix.endsWith(suffix);
        return prefixesNest && suffixesNest;
    }

    @Override
    public String toString() {
        return prefix + PLACEHOLDER + suffix;
    }
}
