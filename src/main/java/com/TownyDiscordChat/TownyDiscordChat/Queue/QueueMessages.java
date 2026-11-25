package com.TownyDiscordChat.TownyDiscordChat.Queue;

/** Human-readable, stable operator messages for durable Discord operations. */
public final class QueueMessages {
    private QueueMessages() { }

    public static String resource(String action, DiscordJobStore.Job job,
                                  String displayName, String discordId) {
        return resource(action,job.kind,displayName,discordId,job.id,job.attempts);
    }

    /** Exposed separately for unit tests without a running Bukkit plugin. */
    public static String resource(String action,String kind,String name,String id,
                                  long jobId,int attempt) {
        String original=kind.replaceFirst("^(DELETE_|RENAME_|RANK_)","");
        String owner=original.startsWith("TOWN_") ? "town"
                : original.startsWith("NATION_") ? "nation" : "Discord";
        String what=original.endsWith("_ROLE") ? "role"
                : original.endsWith("_TEXT") ? "text channel"
                : original.endsWith("_VOICE") ? "voice channel"
                : original.endsWith("_CATEGORY") ? "category" : "job";
        if (kind.startsWith("RANK_")) {
            what+=" moderation";
        }
        String target=name == null ? "" : " \""+name+"\"";
        String remote=id == null || id.isEmpty() ? "" : " [Discord ID "+id+"]";
        return "[QUEUE] " + action + " " + owner + " " + what + target + remote
                + " (job #" + jobId + ", attempt " + attempt + ")";
    }

    /** "[QUEUE] GRANTED town moderation for "Name" [Discord ID 1] in text channel "chat" (job #3, attempt 1)" */
    public static String rank(String action, String channelKind, String channelName,
                              String memberName, long jobId, int attempt) {
        String owner = channelKind.startsWith("TOWN_") ? "town" : "nation";
        String what = channelKind.endsWith("_TEXT") ? "text channel" : "voice channel";
        return "[QUEUE] " + action + " " + owner + " moderation for " + memberName
                + " in " + what + " \"" + channelName + "\""
                + " (job #" + jobId + ", attempt " + attempt + ")";
    }

    public static String member(boolean grant,String member,String role,long jobId,int attempt) {
        return "[QUEUE] " + (grant ? "GRANTED" : "REVOKED") + " " + role
                + " for Discord member " + member
                + " (member job #" + jobId + ", attempt " + attempt + ")";
    }
}
