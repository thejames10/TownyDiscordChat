package com.TownyDiscordChat.TownyDiscordChat.Scheduling;

/** A handle to a repeating task, independent of the server's scheduler type. */
@FunctionalInterface
public interface RepeatingTask {
    void cancel();
}
