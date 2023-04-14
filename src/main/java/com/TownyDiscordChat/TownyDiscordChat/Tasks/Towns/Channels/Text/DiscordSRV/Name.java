package com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.Channels.Text.DiscordSRV;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.List;

public class Name {

    public Name () {

    }

    public List<String> GetAllTownTextChannels() {
        List<String> TextChannels = new ArrayList<>();

        String textCategoriesList = Main.plugin.config.getString("town.TextCategoryId");
        assert textCategoriesList != null;
        String[] textCategories = textCategoriesList.split(",");
        for (String textCategory : textCategories) {
            System.out.println(textCategory);
        }
        return null;
    }
}
