package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-shot, first-start conversion from config.yml.bak to the current
 * bundled config.yml. The source is never edited or renamed.
 *
 * Copies only leaf paths defined by the current config template, keeping
 * new defaults/comments and rejecting incompatible values before any file
 * is published. Existing config.yml always wins, regardless of .bak.
 */
public final class ConfigUpgrade {
    private static final Pattern YAML_KEY =
            Pattern.compile("^(\\s*)([a-zA-Z0-9_-]+):(.*)$");
    private static final Set<String> ROOTS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    "SQL","storage","town","nation","category","log",
                    "messages","queue","onboarding","security")));

    public static final class Result {
        public final boolean migrated;
        public final List<String> copied;
        public final List<String> ignored;

        private Result(boolean migrated,List<String> copied,List<String> ignored) {
            this.migrated=migrated;
            this.copied=Collections.unmodifiableList(new ArrayList<String>(copied));
            this.ignored=Collections.unmodifiableList(new ArrayList<String>(ignored));
        }
    }

    private ConfigUpgrade() { }

    /**
     * Called before JavaPlugin.saveDefaultConfig. If no backup exists, the
     * caller should follow Bukkit's normal default generation path.
     *
     * Throws on malformed/invalid backups WITHOUT creating config.yml.
     * This deliberately puts the plugin in safe mode until an admin fixes
     * the source, instead of quietly switching databases.
     */
    public static Result migrateIfNeeded(File dataFolder,String defaults) throws IOException {
        File destination=new File(dataFolder,"config.yml");
        File backup=new File(dataFolder,"config.yml.bak");
        if (destination.exists() || !backup.isFile())
            return new Result(false,Collections.<String>emptyList(),
                    Collections.<String>emptyList());

        YamlConfiguration oldConfig=new YamlConfiguration();
        YamlConfiguration newConfig=new YamlConfiguration();
        try {
            oldConfig.load(backup);
            newConfig.loadFromString(defaults);
        } catch (InvalidConfigurationException e) {
            throw new IOException("Invalid configuration backup or bundled template: "
                    + e.getMessage(),e);
        }

        Map<String,Object> copied=new HashMap<String,Object>();
        List<String> ignored=new ArrayList<String>();
        for (String path:oldConfig.getKeys(true)) {
            if (oldConfig.isConfigurationSection(path)) continue;
            if (!eligible(path) || !newConfig.contains(path)
                    || newConfig.isConfigurationSection(path)) {
                ignored.add(path);
                continue;
            }
            Object before=oldConfig.get(path);
            Object expected=newConfig.get(path);
            Object safe=compatible(path,before,expected);
            if (safe==null)
                throw new IOException("Backup setting " + path
                        + " has an incompatible type. Fix config.yml.bak or "
                        + "create config.yml manually. No file was migrated.");
            newConfig.set(path,safe);
            copied.put(path,safe);
        }

        if (copied.isEmpty())
            throw new IOException("config.yml.bak contains no recognized settings. "
                    + "Refusing an empty migration; no new config.yml was written.");

        // Never reenable an in-progress offline database transfer based solely
        // on an old backup: that operation must be explicitly requested.
        newConfig.set("storage.migrateToMySQL",false);

        SetupValidator.Result validation=SetupValidator.validateLocal(newConfig);
        if (!validation.valid())
            throw new IOException("Backup settings fail current config validation: "
                    + validation.errors()
                    + ". Fix config.yml.bak or create config.yml manually. "
                    + "No config.yml was written.");

        String rendered=mergeWithComments(defaults,copied);
        // Also validate the final bytes; a mistake in rendering must not
        // result in Bukkit loading a fallback or incomplete configuration.
        YamlConfiguration check=new YamlConfiguration();
        try {
            check.loadFromString(rendered);
        } catch (InvalidConfigurationException e) {
            throw new IOException("Generated configuration failed YAML validation",e);
        }
        for (Map.Entry<String,Object> entry:copied.entrySet()) {
            if (!entry.getValue().equals(check.get(entry.getKey())))
                throw new IOException("Configuration rendering mismatch for "
                        + entry.getKey() + "; no file was written.");
        }

        Path target=destination.toPath();
        if (!Files.isDirectory(target.getParent()))
            Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),"config-upgrade-", ".tmp");
        try {
            Files.write(temp,rendered.getBytes(StandardCharsets.UTF_8));
            // CREATE_NEW behavior: do not clobber a config an admin may have
            // created while we were parsing the backup.
            // No REPLACE_EXISTING: never clobber an admin-created config.
            Files.move(temp,target);
        } finally {
            Files.deleteIfExists(temp);
        }
        List<String> paths=new ArrayList<String>(copied.keySet());
        Collections.sort(paths);
        Collections.sort(ignored);
        return new Result(true,paths,ignored);
    }

    private static boolean eligible(String path) {
        int dot=path.indexOf('.');
        if (dot<=0 || !ROOTS.contains(path.substring(0,dot))) return false;
        return !path.equals("storage.migrateToMySQL");
    }

    private static Object compatible(String path,Object old,Object expected) {
        if (old==null) return null;
        if (path.equals("storage.type")) {
            if (!(old instanceof String)) return null;
            String selection=((String)old).trim().toLowerCase(java.util.Locale.ROOT);
            return Arrays.asList("auto","sqlite","mysql").contains(selection)
                    ? selection : null;
        }
        if (expected instanceof Boolean)
            return old instanceof Boolean ? old : null;
        if (expected instanceof Number) {
            if (path.endsWith("CategoryId") || path.equals("messages.DiscordLogTextChannelId")) {
                if (old instanceof String || old instanceof Number)
                    return String.valueOf(old);
                return null;
            }
            if (!(old instanceof Number)) return null;
            double val=((Number)old).doubleValue();
            if (!Double.isFinite(val) || val!=Math.rint(val)
                    || val<Integer.MIN_VALUE || val>Integer.MAX_VALUE) return null;
            return Integer.valueOf(((Number)old).intValue());
        }
        if (expected instanceof String) {
            if (path.equals("SQL.port") && old instanceof Number)
                return String.valueOf(old);
            return old instanceof String ? old : null;
        }
        return null;
    }

    /**
     * Replace only scalar values in the current template, leaving the latest
     * comments and newly introduced settings in their original positions.
     * All values are YAML double-quoted strings or typed booleans/numbers.
     */
    static String mergeWithComments(String defaults,Map<String,Object> overrides)
            throws IOException {
        // Git checkouts on Windows may use CRLF. Match YAML keys without
        // trailing carriage returns, then retain the template newline style.
        String lineEnding=defaults.contains("\r\n") ? "\r\n"
                : (defaults.contains("\n") ? "\n" : "\r");
        String[] lines=defaults.split("\\r\\n|\\n|\\r",-1);
        List<String> parents=new ArrayList<String>();
        List<Integer> levels=new ArrayList<Integer>();
        Set<String> seen=new HashSet<String>();
        for (int i=0;i<lines.length;i++) {
            Matcher m=YAML_KEY.matcher(lines[i]);
            if (!m.matches()) continue;
            String indent=m.group(1), name=m.group(2);
            int level=indent.length();
            while (!levels.isEmpty() && levels.get(levels.size()-1)>=level) {
                levels.remove(levels.size()-1);
                parents.remove(parents.size()-1);
            }
            String path=String.join(".",parents)+(parents.isEmpty()?"":".")+name;
            if (overrides.containsKey(path)) {
                lines[i]=indent+name+": "+scalar(overrides.get(path));
                seen.add(path);
            }
            // A section's value has no scalar (only whitespace or comment).
            String tail=m.group(3).trim();
            if (tail.isEmpty() || tail.startsWith("#")) {
                parents.add(name);
                levels.add(level);
            }
        }
        if (seen.size()!=overrides.size())
            throw new IOException("Current config template lacks migrated setting(s): "
                    + difference(overrides.keySet(),seen));
        return String.join(lineEnding,lines);
    }

    private static Set<String> difference(Set<String> expected,Set<String> actual) {
        Set<String> missing=new HashSet<String>(expected);
        missing.removeAll(actual);
        return missing;
    }

    private static String scalar(Object value) {
        if (value instanceof Boolean || value instanceof Number)
            return String.valueOf(value);
        StringBuilder result=new StringBuilder("\"");
        String text=String.valueOf(value);
        for (int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            switch(c) {
                case '"': result.append("\\\""); break;
                case '\\': result.append("\\\\"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\t': result.append("\\t"); break;
                default:
                    if (c<32 || c==127)
                        result.append(String.format("\\u%04x",(int)c));
                    else result.append(c);
            }
        }
        return result.append('"').toString();
    }
}
