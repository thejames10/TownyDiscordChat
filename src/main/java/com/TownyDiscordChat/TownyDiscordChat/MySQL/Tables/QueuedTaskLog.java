package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

public class QueuedTaskLog extends SQL {

    private final Main plugin;

    private final String TABLE = "tdc_queued_tasks_log";

    public QueuedTaskLog(Main plugin) {
        this.plugin = plugin;
    }
}
