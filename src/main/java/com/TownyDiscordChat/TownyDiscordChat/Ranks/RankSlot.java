package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One member's moderation permissions on one town or nation channel. */
public final class RankSlot {
    public static final String JOB_PREFIX = "RANK_";
    private static final Pattern CHANNEL_KIND = Pattern.compile("(TOWN|NATION)_(TEXT|VOICE)");
    private static final Pattern JOB_NAME = Pattern.compile("([0-9]{1,20}):([0-9]{1,20})");

    private final String channelKind;
    private final String channelId;
    private final String memberId;

    public RankSlot(String channelKind, String channelId, String memberId) {
        this.channelKind = channelKind;
        this.channelId = channelId;
        this.memberId = memberId;
    }

    /** Null when the job was not written by this class. */
    public static RankSlot fromJob(String jobKind, String jobName) {
        if (jobKind == null || jobName == null || !jobKind.startsWith(JOB_PREFIX)) {
            return null;
        }
        String channelKind = jobKind.substring(JOB_PREFIX.length());
        Matcher name = JOB_NAME.matcher(jobName);
        if (!CHANNEL_KIND.matcher(channelKind).matches() || !name.matches()) {
            return null;
        }
        return new RankSlot(channelKind, name.group(1), name.group(2));
    }

    public String channelKind() {
        return channelKind;
    }

    public String channelId() {
        return channelId;
    }

    public String memberId() {
        return memberId;
    }

    public boolean isTown() {
        return channelKind.startsWith("TOWN_");
    }

    public boolean isText() {
        return channelKind.endsWith("_TEXT");
    }

    public String jobKind() {
        return JOB_PREFIX + channelKind;
    }

    public String jobName() {
        return channelId + ":" + memberId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RankSlot)) {
            return false;
        }
        RankSlot slot = (RankSlot) other;
        return channelKind.equals(slot.channelKind) && channelId.equals(slot.channelId)
                && memberId.equals(slot.memberId);
    }

    @Override
    public int hashCode() {
        return (channelKind.hashCode() * 31 + channelId.hashCode()) * 31 + memberId.hashCode();
    }

    @Override
    public String toString() {
        return jobKind() + ":" + jobName();
    }
}
