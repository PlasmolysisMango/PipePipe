package com.plasmolysismango.aacg;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.plasmolysismango.ppipecore.Aes;
import com.plasmolysismango.ppipecore.Base64Codec;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Extracts the encrypted mirror routes from the entry page's {@code window.appConfig}. */
public final class AacgConfigParser {
    public static final class NoConfigException extends Exception {
        NoConfigException(final String message) {
            super(message);
        }
    }

    public static final class Target {
        public final String name;
        public final String url;
        public final String badge;
        public final String kind;

        Target(final String name, final String url, final String badge, final String kind) {
            this.name = name;
            this.url = url;
            this.badge = badge;
            this.kind = kind;
        }
    }

    private static final String[] ALLOWED_SCRIPT_TYPES = {
            "", "module", "text/javascript", "application/javascript",
            "text/ecmascript", "application/ecmascript"
    };

    private AacgConfigParser() {
    }

    public static List<Target> parse(final Document document) throws NoConfigException {
        if (document == null) {
            throw new NoConfigException("No document");
        }
        String data = null;
        String key = null;
        int assignments = 0;
        for (final Element script : document.select("script:not([src])")) {
            if (!isScriptTypeAllowed(script.attr("type"))) {
                continue;
            }
            final String text = script.data();
            // Unrelated analytics scripts can use syntax outside this literal-only parser.
            if (!text.contains("appConfig")) {
                continue;
            }
            final List<AacgJsTokenizer.Token> tokens;
            try {
                tokens = AacgJsTokenizer.tokenize(text);
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid inline script: " + e.getMessage(), e);
            }
            int depth = 0;
            for (int i = 0; i < tokens.size(); i++) {
                if (tokenIs(tokens, i, "window") && !tokenIs(tokens, i - 1, ".")) {
                    int end = i + 3;
                    final boolean dot = tokenIs(tokens, i + 1, ".")
                            && tokenIs(tokens, i + 2, "appConfig");
                    final boolean bracket = tokenIs(tokens, i + 1, "[")
                            && i + 3 < tokens.size() && tokens.get(i + 2).quoted
                            && "appConfig".equals(tokens.get(i + 2).text)
                            && tokenIs(tokens, i + 3, "]");
                    if (bracket) {
                        end++;
                    }
                    if ((dot || bracket) && end < tokens.size()) {
                        final boolean plain = tokenIs(tokens, end, "=")
                                && !tokenIs(tokens, end + 1, "=")
                                && !tokenIs(tokens, end + 1, ">");
                        final boolean modified = configMutation(tokens, end);
                        if (plain || modified) {
                            assignments++;
                            if (assignments > 1) {
                                throw new IllegalArgumentException(
                                        "Multiple window.appConfig assignments");
                            }
                            if (!plain || bracket || depth != 0
                                    || i > 0 && !tokenIs(tokens, i - 1, ";")
                                    && !tokenIs(tokens, i - 1, "}")) {
                                throw new IllegalArgumentException("Unsupported window.appConfig "
                                        + "assignment; require a standalone top-level literal");
                            }
                            final String[] literal = configLiteral(tokens, end + 1);
                            data = literal[0];
                            key = literal[1];
                        }
                    }
                }
                if (!tokens.get(i).quoted) {
                    final String token = tokens.get(i).text;
                    if ("{".equals(token) || "[".equals(token) || "(".equals(token)) {
                        depth++;
                    } else if ("}".equals(token) || "]".equals(token)
                            || ")".equals(token)) {
                        depth--;
                    }
                }
            }
        }
        if (assignments == 0) {
            throw new NoConfigException("No inline window.appConfig assignment");
        }
        final String payload;
        try {
            payload = decodeConfig(data, key);
        } catch (final IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid appConfig payload: "
                    + e.getMessage(), e);
        }
        return targets(payload);
    }

    /** Decrypts the base64 payload: first 16 bytes are the IV, SHA-256(key) is the AES key. */
    public static String decodeConfig(final String data, final String key)
            throws GeneralSecurityException {
        if (data == null || data.trim().isEmpty() || key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Config data and key must be nonempty");
        }
        final byte[] encoded = Base64Codec.decodeStrict(data.trim());
        if (encoded.length < 16) {
            throw new IllegalArgumentException("Config has a short IV");
        }
        final byte[] iv = new byte[16];
        System.arraycopy(encoded, 0, iv, 0, 16);
        final byte[] ciphertext = new byte[encoded.length - 16];
        System.arraycopy(encoded, 16, ciphertext, 0, ciphertext.length);
        if (ciphertext.length == 0 || ciphertext.length % 16 != 0) {
            throw new IllegalArgumentException(
                    "Config ciphertext must be nonempty and block-aligned");
        }
        final byte[] aesKey = MessageDigest.getInstance("SHA-256")
                .digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Aes.cbcDecryptUtf8(aesKey, iv, ciphertext);
    }

    public static List<Target> targets(final String payload) {
        final JsonObject config;
        try {
            config = JsonParser.object().from(payload);
        } catch (final JsonParserException e) {
            throw new IllegalArgumentException("Invalid config JSON", e);
        }
        final List<List<JsonObject>> groups = new ArrayList<>();
        groups.add(routeGroup(config.get("domain"), "domain"));
        groups.add(routeGroup(config.get("backup_domain"), "backup_domain"));
        int total = 0;
        for (final List<JsonObject> group : groups) {
            total += group.size();
        }
        if (total > 32) {
            throw new IllegalArgumentException("Config exceeds 32 route entries");
        }
        final String[] kinds = {"primary", "backup"};
        final List<Target> result = new ArrayList<>();
        final Map<String, Boolean> seen = new LinkedHashMap<>();
        for (int g = 0; g < groups.size(); g++) {
            final List<JsonObject> group = groups.get(g);
            for (int index = 0; index < group.size(); index++) {
                final JsonObject route = group.get(index);
                final String name = stringField(route, "name");
                final String value = stringField(route, "value");
                if (name.trim().isEmpty() || value.trim().isEmpty()) {
                    throw new IllegalArgumentException(kinds[g] + " route " + (index + 1)
                            + " requires nonempty name and value");
                }
                final String normalized;
                try {
                    normalized = AacgUrls.normalize(value);
                } catch (final IllegalArgumentException e) {
                    throw new IllegalArgumentException(kinds[g] + " route " + (index + 1)
                            + " has an invalid URL", e);
                }
                if (!seen.containsKey(normalized)) {
                    seen.put(normalized, Boolean.TRUE);
                    result.add(new Target(name, normalized, stringField(route, "badge"),
                            kinds[g]));
                }
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Config contains no explicit routes");
        }
        return result;
    }

    private static List<JsonObject> routeGroup(final Object value, final String name) {
        final List<JsonObject> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        if (value instanceof JsonArray) {
            final JsonArray array = (JsonArray) value;
            for (int i = 0; i < array.size(); i++) {
                final Object item = array.get(i);
                if (item instanceof JsonObject) {
                    result.add((JsonObject) item);
                } else {
                    throw new IllegalArgumentException(name + " must contain objects");
                }
            }
            return result;
        }
        if (value instanceof JsonObject) {
            result.add((JsonObject) value);
            return result;
        }
        throw new IllegalArgumentException(name + " must be an array"
                + ("backup_domain".equals(name) ? " or object" : ""));
    }

    private static String stringField(final JsonObject object, final String key) {
        final Object value = object.get(key);
        return value instanceof String ? (String) value : "";
    }

    private static boolean isScriptTypeAllowed(final String raw) {
        final String type = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
        for (final String allowed : ALLOWED_SCRIPT_TYPES) {
            if (allowed.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tokenIs(final List<AacgJsTokenizer.Token> tokens, final int index,
                                   final String text) {
        if (index < 0 || index >= tokens.size()) {
            return false;
        }
        final AacgJsTokenizer.Token token = tokens.get(index);
        return !token.quoted && text.equals(token.text);
    }

    private static boolean configMutation(final List<AacgJsTokenizer.Token> tokens,
                                          final int start) {
        final StringBuilder operator = new StringBuilder();
        for (int end = start; end < tokens.size() && end < start + 4; end++) {
            if (tokens.get(end).quoted) {
                break;
            }
            operator.append(tokens.get(end).text);
            switch (operator.toString()) {
                case "++":
                case "--":
                case "+=":
                case "-=":
                case "*=":
                case "**=":
                case "%=":
                case "&=":
                case "&&=":
                case "|=":
                case "||=":
                case "^=":
                case "??=":
                case "<<=":
                case ">>=":
                case ">>>=":
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    private static String[] configLiteral(final List<AacgJsTokenizer.Token> tokens,
                                          final int start) {
        int i = start;
        if (!tokenIs(tokens, i, "{")) {
            throw new IllegalArgumentException(
                    "window.appConfig must be a literal object");
        }
        final Map<String, String> fields = new LinkedHashMap<>();
        i++;
        while (true) {
            if (i >= tokens.size()) {
                throw new IllegalArgumentException(
                        "window.appConfig object is not terminated");
            }
            if (tokenIs(tokens, i, "}")) {
                break;
            }
            if (i + 2 >= tokens.size() || !tokenIs(tokens, i + 1, ":")
                    || !tokens.get(i + 2).quoted
                    || !AacgJsTokenizer.isIdentifier(tokens.get(i))) {
                throw new IllegalArgumentException(
                        "appConfig properties must have literal string values");
            }
            final String name = tokens.get(i).text;
            if (fields.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate appConfig property");
            }
            fields.put(name, tokens.get(i + 2).text);
            i += 3;
            if (tokenIs(tokens, i, "}")) {
                break;
            }
            if (!tokenIs(tokens, i, ",")) {
                throw new IllegalArgumentException(
                        "Unsupported appConfig value expression");
            }
            i++;
        }
        if (i + 1 < tokens.size() && !tokenIs(tokens, i + 1, ";")) {
            throw new IllegalArgumentException("Unsupported appConfig assignment suffix");
        }
        final String data = fields.get("data");
        final String key = fields.get("key");
        if (data == null || data.trim().isEmpty() || key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("appConfig requires nonempty data and key");
        }
        return new String[]{data, key};
    }
}
