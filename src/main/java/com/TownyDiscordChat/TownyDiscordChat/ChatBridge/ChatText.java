package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text conversion between Minecraft chat and Discord, safe in both directions. */
public final class ChatText {
    private static final Pattern COLOR_CODES = Pattern.compile("(?i)[§&][0-9a-fk-orx]");
    private static final Pattern MARKDOWN = Pattern.compile("([\\\\*_~`|>])");
    private static final Pattern LINE_BREAKS = Pattern.compile("\\s*[\\r\\n]+\\s*");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(player|message)}");
    private static final String SECTION_SIGN = "§";
    // A zero-width space after @ stops @everyone, @here and <@id> from pinging.
    private static final String MENTION_BREAK = "@​";
    private static final String ELLIPSIS = "…";

    private ChatText() {
    }

    /** Minecraft chat ready to post: no colour codes, Markdown or mentions. */
    public static String forDiscord(String minecraftText, int maxLength) {
        String plain = COLOR_CODES.matcher(minecraftText).replaceAll("").trim();
        return neutralizeMentions(escapeMarkdown(truncate(plain, maxLength)));
    }

    /** Discord text ready to show in game: one line, no section-sign colour codes. */
    public static String forMinecraft(String discordText, int maxLength) {
        String oneLine = LINE_BREAKS.matcher(discordText).replaceAll(" ").trim();
        return truncate(oneLine.replace(SECTION_SIGN, ""), maxLength);
    }

    public static String escapeMarkdown(String text) {
        return MARKDOWN.matcher(text).replaceAll("\\\\$1");
    }

    public static String neutralizeMentions(String text) {
        return text.replace("@", MENTION_BREAK);
    }

    /** Fills {player} and {message} in one pass, so a message cannot add a placeholder. */
    public static String format(String template, String player, String message) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("player", player);
        values.put("message", message);
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer filled = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(filled, Matcher.quoteReplacement(values.get(matcher.group(1))));
        }
        matcher.appendTail(filled);
        return filled.toString();
    }

    static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int end = Math.max(0, maxLength - ELLIPSIS.length());
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end) + ELLIPSIS;
    }
}
