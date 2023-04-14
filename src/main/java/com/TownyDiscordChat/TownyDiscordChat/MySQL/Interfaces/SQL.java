package com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.TownyDiscordChat.TownyDiscordChat.Main.plugin;

public class SQL {

    public static final String TABLE_PLAYERS = "tdc_players";
    public static final String TABLE_NATIONS = "tdc_nations";
    public static final String TABLE_TOWNS = "tdc_towns";
    public static final String TABLE_QUEUED_TASKS = "tdc_queued_tasks";
    public static final String TABLE_TOWN_TEXT_CHANNELS = "tdc_town_text_channels";
    public static final String TABLE_NATION_TEXT_CHANNELS = "tdc_nation_text_channels";
    public static final String TABLE_TOWN_VOICE_CHANNELS = "tdc_town_voice_channels";
    public static final String TABLE_NATION_VOICE_CHANNELS = "tdc_nation_voice_channels";

    public void createEntry (String searchValue, String searchColName, String TABLE, Map<String,String> ColNameValuePair, Boolean ignoreAlreadyExists) {

        try {
            if (!entryExists(searchValue, searchColName, TABLE)) {

                int numQuestionMarks = ColNameValuePair.size();
                String values = IntStream.range(0, numQuestionMarks)
                        .mapToObj(i -> "?")
                        .collect(Collectors.joining(","));

                StringBuilder columns = new StringBuilder();
                int i = 1;
                for (var value : ColNameValuePair.entrySet()) {
                    if (i < ColNameValuePair.size()) {
                        columns.append(value.getKey()).append(",");
                    } else {
                        columns.append(value.getKey());
                    }
                    i++;
                }

                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(" + columns + ") VALUES (" + values + ")");

                i = 1;
                for (var value : ColNameValuePair.entrySet()) {
                    ps.setString(i, value.getValue());
                    i++;
                }
                ps.executeUpdate();
            } else if (!ignoreAlreadyExists) {

                StringBuilder values = new StringBuilder();
                int i = 1;
                for (var value : ColNameValuePair.entrySet()) {
                    if (i < ColNameValuePair.size()) {
                        values.append(value.getKey()).append(" = '").append(value.getValue()).append("', ");
                    } else {
                        values.append(value.getKey()).append(" = '").append(value.getValue()).append("'");
                    }
                    i++;
                }

                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET "
                        + values + " WHERE " + searchColName + " = '" + searchValue + "'");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEntry(String searchValue, String searchColName, String TABLE) {
        if (entryExists(searchValue, searchColName, TABLE)) {
            try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("DELETE FROM " + TABLE + " "
                        + "WHERE " + searchColName + "=?");
                ps.setString(1, searchValue);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean entryExists(String searchValue, String searchColName, String TABLE) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT * FROM " + TABLE + " WHERE " + searchColName + "=?");
            ps.setString(1, searchValue);

            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                // entry is found
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName, String TABLE) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET " + setCol + "=? WHERE " + searchColName + "=?");
            ps.setString(1, setValue);
            ps.setString(2, searchValue);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*public void updateEntry(String setValue, String setCol, String searchValue, String searchColName, String TABLE) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET " + setCol1 + "=?, " + setCol2 + "=? WHERE " + searchColName + "=?");
            ps.setString(1, setValue);
            ps.setString(2, setValue2);
            ps.setString(3, searchValue);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName, String TABLE) {
        if (entryExists(searchValue, searchColName, TABLE)) {
            try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT " + selectCol + " FROM " + TABLE + " WHERE " + searchColName + "=?");
                ps.setString(1, searchValue);

                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) { return resultSet.getString(1); }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public @Nullable List<String> getAllEntries(String selectCol, String TABLE) {
            try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT " + selectCol + " FROM " + TABLE );

                ResultSet resultSet = ps.executeQuery();

                List<String> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(resultSet.getString(selectCol));
                }
                return results;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        return null;
    }

    public @Nullable List<String> getAllColumnEntries(String selectCol, String TABLE) {
            try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT " + selectCol + " FROM " + TABLE);

                ResultSet resultSet = ps.executeQuery();

                List<String> list = new ArrayList<>();
                while (resultSet.next()){
                    list.add(resultSet.getString(selectCol));
                }
                return list;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        return null;
    }

    public @Nullable static Map<String,String> getQueueFirst () {
        Map<String,String> result = new HashMap<>();

        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT * FROM tdc_queued_tasks WHERE Expired IS NULL ORDER BY CAST(Time AS SIGNED) LIMIT 1");

            ResultSet resultSet = ps.executeQuery();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            boolean foundValue = false;

            while (resultSet.next()) {
                int numColumns = rsmd.getColumnCount();
                for (int i = 1; i <= numColumns; i++) {
                    Object columnValue = resultSet.getObject(i);
                    if (resultSet.wasNull()) {
                        System.out.println("Value in column " + rsmd.getColumnName(i) + " is NULL");
                    } else {
                        System.out.println("Value in column " + rsmd.getColumnName(i) + " is not NULL: " + columnValue);

                        if (rsmd.getColumnName(i).equalsIgnoreCase("Time")) {
                            result.put(rsmd.getColumnName(i), columnValue.toString());
                        }

                        if (!rsmd.getColumnName(i).equalsIgnoreCase("Time")) {

                            result.put(rsmd.getColumnName(i), columnValue.toString());

                            foundValue = true;
                            break;
                        }
                    }
                }
                if (foundValue) { break; }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (result.isEmpty()) { return null; }
        else {return result; }
    }

    public static long DateTimeToLong () {
        LocalDateTime dateTime = LocalDateTime.now();
        long timestamp = dateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000; // convert to milliseconds
        System.out.println(timestamp);

        return timestamp;
    }

    public static long DateTimeToLong (@NotNull LocalDateTime dateTime) {
        long timestamp = dateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000; // convert to milliseconds
        System.out.println(timestamp);

        return timestamp;
    }

    public static LocalDateTime LongToDateTime (long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime dateTime = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
        System.out.println(dateTime);

        return dateTime;
    }

}
