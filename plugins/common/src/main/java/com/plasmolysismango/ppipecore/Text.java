package com.plasmolysismango.ppipecore;

import java.util.HashMap;
import java.util.Map;

/** Text helpers: entity decoding, whitespace collapsing, durations, title cleanup. */
public final class Text {
    private Text() {
    }

    private static final Map<String, String> ENTITIES = new HashMap<>();

    static {
        ENTITIES.put("amp", "&");
        ENTITIES.put("quot", "\"");
        ENTITIES.put("apos", "'");
        ENTITIES.put("lt", "<");
        ENTITIES.put("gt", ">");
        ENTITIES.put("nbsp", " ");
        ENTITIES.put("hellip", "...");
        ENTITIES.put("mdash", "-");
        ENTITIES.put("ndash", "-");
    }

    /** Decodes the common named and numeric HTML entities. */
    public static String unescapeHtml(final String value) {
        if (value == null || value.indexOf('&') < 0) {
            return value;
        }
        final StringBuilder out = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            final char c = value.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            final int semicolon = value.indexOf(';', i + 1);
            if (semicolon < 0 || semicolon - i > 10) {
                out.append(c);
                i++;
                continue;
            }
            final String entity = value.substring(i + 1, semicolon);
            final String replacement = decodeEntity(entity);
            if (replacement == null) {
                out.append(c);
                i++;
            } else {
                out.append(replacement);
                i = semicolon + 1;
            }
        }
        return out.toString();
    }

    private static String decodeEntity(final String entity) {
        if (entity.startsWith("#")) {
            try {
                final int code = entity.startsWith("#x") || entity.startsWith("#X")
                        ? Integer.parseInt(entity.substring(2), 16)
                        : Integer.parseInt(entity.substring(1));
                if (code > 0 && code <= Character.MAX_CODE_POINT) {
                    return new String(Character.toChars(code));
                }
            } catch (final NumberFormatException ignored) {
                return null;
            }
            return null;
        }
        return ENTITIES.get(entity);
    }

    public static String clean(final String value) {
        return value == null ? "" : collapseWhitespace(unescapeHtml(value)).trim();
    }

    public static String collapseWhitespace(final String value) {
        if (value == null) {
            return "";
        }
        final StringBuilder out = new StringBuilder(value.length());
        boolean pendingSpace = false;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (Character.isWhitespace(c) || c == '\u00A0') {
                pendingSpace = out.length() > 0;
            } else {
                if (pendingSpace) {
                    out.append(' ');
                    pendingSpace = false;
                }
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Parses "12:34", "1:02:03", "123分钟", "2小时" into seconds, or 0 when unknown. */
    public static long parseDurationSeconds(final String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        final String value = text.trim();
        final java.util.regex.Matcher hms = java.util.regex.Pattern
                .compile("(\\d{1,2}):(\\d{2}):(\\d{2})").matcher(value);
        if (hms.find()) {
            return Long.parseLong(hms.group(1)) * 3600L
                    + Long.parseLong(hms.group(2)) * 60L
                    + Long.parseLong(hms.group(3));
        }
        final java.util.regex.Matcher ms = java.util.regex.Pattern
                .compile("(\\d{1,2}):(\\d{2})").matcher(value);
        if (ms.find()) {
            return Long.parseLong(ms.group(1)) * 60L + Long.parseLong(ms.group(2));
        }
        long total = 0;
        final java.util.regex.Matcher hours = java.util.regex.Pattern
                .compile("(\\d+)\\s*(?:小时|h\\b)").matcher(value);
        if (hours.find()) {
            total += Long.parseLong(hours.group(1)) * 3600L;
        }
        final java.util.regex.Matcher minutes = java.util.regex.Pattern
                .compile("(\\d+)\\s*分").matcher(value);
        if (minutes.find()) {
            total += Long.parseLong(minutes.group(1)) * 60L;
        }
        return total;
    }

    public static String stripCodeFromTitle(final String title, final String code) {
        if (title == null) {
            return "";
        }
        if (code == null || code.isEmpty()) {
            return title.trim();
        }
        final int index = title.indexOf(code);
        if (index < 0) {
            return title.trim();
        }
        return (title.substring(0, index) + title.substring(index + code.length())).trim();
    }
}
