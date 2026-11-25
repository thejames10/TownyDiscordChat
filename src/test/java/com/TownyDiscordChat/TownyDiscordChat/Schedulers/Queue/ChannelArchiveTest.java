package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue;

import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.GuildChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.PermissionOverride;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelArchiveTest {
    private static final String ARCHIVE_NAME = "MINECRAFT-ARCHIVE";
    private final Role everyone = fake(Role.class, new HashMap<String, Object>());

    @Test
    void archiveIsOffByDefault() {
        assertFalse(ChannelArchive.enabled(new YamlConfiguration()));
    }

    @Test
    void privateCategoryWithRoomIsUsed() {
        Category archive = category(true, 3);
        Guild guild = guild(archive);

        assertSame(archive, ChannelArchive.availableCategory(guild, config(50)));
        assertFalse(ChannelArchive.hasPublicCategory(guild, config(50)));
    }

    @Test
    void fullCategoryIsSkipped() {
        Category full = category(true, 2);
        Category spare = category(true, 1);

        assertSame(spare, ChannelArchive.availableCategory(guild(full, spare), config(2)));
        assertNull(ChannelArchive.availableCategory(guild(full), config(2)));
    }

    @Test
    void publicCategoryIsNeverUsed() {
        Category visible = category(false, 0);
        Guild guild = guild(visible);

        assertNull(ChannelArchive.availableCategory(guild, config(50)));
        assertTrue(ChannelArchive.hasPublicCategory(guild, config(50)));
    }

    private YamlConfiguration config(int capacity) {
        YamlConfiguration config = new YamlConfiguration();
        config.set(ChannelArchive.ENABLED_KEY, true);
        config.set(ChannelArchive.CATEGORY_KEY, ARCHIVE_NAME);
        config.set("category.SubChannelsMax", capacity);
        return config;
    }

    private Guild guild(Category... categories) {
        Map<String, Object> answers = new HashMap<String, Object>();
        answers.put("getCategoriesByName", Arrays.asList(categories));
        answers.put("getPublicRole", everyone);
        return fake(Guild.class, answers);
    }

    private Category category(boolean hiddenFromEveryone, int channelCount) {
        Map<String, Object> overrideAnswers = new HashMap<String, Object>();
        overrideAnswers.put("getDenied", hiddenFromEveryone
                ? EnumSet.of(Permission.VIEW_CHANNEL) : EnumSet.noneOf(Permission.class));
        List<GuildChannel> channels = new ArrayList<GuildChannel>(Collections.<GuildChannel>nCopies(
                channelCount, fake(GuildChannel.class, new HashMap<String, Object>())));
        Map<String, Object> answers = new HashMap<String, Object>();
        answers.put("getPermissionOverride", fake(PermissionOverride.class, overrideAnswers));
        answers.put("getChannels", channels);
        return fake(Category.class, answers);
    }

    private static <T> T fake(Class<T> type, Map<String, Object> answers) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (self, method, arguments) -> answers.get(method.getName()));
        return type.cast(proxy);
    }
}
