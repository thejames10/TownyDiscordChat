package com.TownyDiscordChat.TownyDiscordChat.Storage;

import java.util.regex.Pattern;

/**
 * Small, intentionally restricted translation layer for SQL used by the
 * existing TownyDiscordChat stores. The SQL branch originally used MySQL
 * upsert and DDL syntax; SQLite has equivalent constraints and upsert support.
 *
 * Do not pass arbitrary user-supplied SQL through here.
 */
public final class SqlDialect {
    private static final Pattern MYSQL_ENGINE = Pattern.compile(
            "\\s+ENGINE=InnoDB(?:\\s+DEFAULT\\s+CHARSET=utf8mb4)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTOINCREMENT = Pattern.compile(
            "BIGINT\\s+NOT\\s+NULL\\s+AUTO_INCREMENT\\s+PRIMARY\\s+KEY",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_IGNORE = Pattern.compile(
            "\\bINSERT\\s+IGNORE\\s+INTO\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPSERT = Pattern.compile(
            "\\bON\\s+DUPLICATE\\s+KEY\\s+UPDATE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUES = Pattern.compile(
            "\\bVALUES\\s*\\(\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCK = Pattern.compile(
            "\\s+FOR\\s+UPDATE\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIQUE_KEY = Pattern.compile(
            ",\\s*UNIQUE\\s+KEY\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(([^)]*)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INDEX = Pattern.compile(
            ",\\s*(?:INDEX|KEY)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^)]*\\)",
            Pattern.CASE_INSENSITIVE);

    private SqlDialect() { }

    public static String sqlite(String statement) {
        if (statement == null) throw new IllegalArgumentException("SQL cannot be null");
        String sql = MYSQL_ENGINE.matcher(statement).replaceAll("");
        sql = AUTOINCREMENT.matcher(sql).replaceAll("INTEGER PRIMARY KEY AUTOINCREMENT");
        sql = UNIQUE_KEY.matcher(sql).replaceAll(", UNIQUE ($1)");
        sql = INDEX.matcher(sql).replaceAll("");
        sql = INSERT_IGNORE.matcher(sql).replaceAll("INSERT OR IGNORE INTO");
        sql = LOCK.matcher(sql).replaceAll("");
        sql = UPSERT.matcher(sql).replaceAll("ON CONFLICT DO UPDATE SET");
        sql = VALUES.matcher(sql).replaceAll("excluded.$1");
        return rewriteIfFunctions(sql);
    }

    /**
     * MySQL IF(condition, whenTrue, whenFalse) can nest inside another IF.
     * Convert only function calls (not identifiers or quoted literals) into
     * portable CASE WHEN expressions. The parser understands SQL quotes and
     * balanced parentheses; non-three-argument calls fail closed.
     */
    static String rewriteIfFunctions(String sql) {
        StringBuilder output = new StringBuilder();
        char quote = 0;
        for (int i=0; i<sql.length();) {
            char c=sql.charAt(i);
            if (quote!=0) {
                output.append(c);
                if (c==quote) {
                    if (i+1<sql.length() && sql.charAt(i+1)==quote) {
                        output.append(quote);
                        i+=2;
                        continue;
                    }
                    quote=0;
                }
                i++;
                continue;
            }
            if (c=='\'' || c=='"' || c=='`') { quote=c; output.append(c); i++; continue; }
            if (i+3<=sql.length()
                    && sql.regionMatches(true,i,"IF(",0,3)
                    && (i==0 || !Character.isLetterOrDigit(sql.charAt(i-1)))) {
                int start=i+3;
                int depth=1;
                char innerQuote=0;
                java.util.List<String> args = new java.util.ArrayList<String>();
                int part=start, j=start;
                for (;j<sql.length();j++) {
                    char ch=sql.charAt(j);
                    if (innerQuote!=0) {
                        if (ch==innerQuote) {
                            if (j+1<sql.length() && sql.charAt(j+1)==innerQuote) { j++; continue; }
                            innerQuote=0;
                        }
                        continue;
                    }
                    if (ch=='\'' || ch=='"' || ch=='`') { innerQuote=ch; continue; }
                    if (ch=='(') depth++;
                    else if (ch==')') {
                        depth--;
                        if(depth==0) { args.add(sql.substring(part,j)); break; }
                    } else if(ch==',' && depth==1) {
                        args.add(sql.substring(part,j));
                        part=j+1;
                    }
                }
                if(depth!=0 || args.size()!=3)
                    throw new IllegalArgumentException("Unrecognized IF() expression in SQL dialect");
                output.append("(CASE WHEN ")
                    .append(rewriteIfFunctions(args.get(0).trim()))
                    .append(" THEN ").append(rewriteIfFunctions(args.get(1).trim()))
                    .append(" ELSE ").append(rewriteIfFunctions(args.get(2).trim()))
                    .append(" END)");
                i=j+1;
            } else {
                output.append(c);
                i++;
            }
        }
        return output.toString();
    }
}
