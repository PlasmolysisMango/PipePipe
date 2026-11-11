package com.plasmolysismango.aacg;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AacgConfigParserTest {
    // Generated with Node crypto by the original project, to catch mirrored decoder bugs.
    private static final String KNOWN_DATA =
            "AAECAwQFBgcICQoLDA0OD0H1fNDrxsZ2eaVQNyez7HJDxAnLDQn1ieXDaFQ0MMUW/Nz3DyK6ymJBi5IwSApQk5XZ1fRfFQj0l2w6tolT3KsFy+Hf/GAO0ClqbCL48F4S2FfzBjWqi3kaaPuv2nb3NhlYRPsQ8/+TSzAo5hQUuIqFyZRxV+uR+FgPN+N71zF/oSgOMFsWNFTHMwaQSxv/RFgOaOywBADCnmzTLJofLglzItXZzgj5kDLg0x8uit8hrYJan9bwYTVE8Fe9+Y2GWCfwrdRI1mgkim2a4Dnzu734Cr+1Y3vXj5Tz3SHytSmp43vkaw/r6wSl4OF/SOigeVE8W8KSPujSUIsUQEZGWgfZ1cCrllHZMUhrjlie7rdj1P8AyjDMDbnXftuSxDXcS5kQvJBpnyg43FpAsrWH9Ec=";
    private static final String KNOWN_KEY = "synthetic-key-雪";
    private static final String KNOWN_JSON = "{\"domain\":[{\"name\":\"Primary\","
            + "\"value\":\"https://EXAMPLE.invalid/primary#landing\",\"badge\":"
            + "\"Recommended\"}],\"backup_domain\":{\"name\":\"Backup\",\"value\":"
            + "\"https://backup.example.invalid/\",\"badge\":\"\"},"
            + "\"randomDomain\":\"1\",\"zz_line\":\"unused.example.invalid\","
            + "\"zz_backup_line\":\"unused-backup.example.invalid\"}";

    @Test
    void decodesKnownAnswer() throws Exception {
        final String payload = AacgConfigParser.decodeConfig(KNOWN_DATA, KNOWN_KEY);
        assertEquals(KNOWN_JSON, payload);
    }

    @Test
    void parsesRoutes() {
        final Document document = Jsoup.parse("<script>window.appConfig = {data:'"
                + KNOWN_DATA + "', key:'" + KNOWN_KEY + "'};</script>");
        try {
            final List<AacgConfigParser.Target> targets = AacgConfigParser.parse(document);
            assertEquals(2, targets.size());
            assertEquals("Primary", targets.get(0).name);
            assertEquals("https://example.invalid/primary", targets.get(0).url);
            assertEquals("Recommended", targets.get(0).badge);
            assertEquals("primary", targets.get(0).kind);
            assertEquals("Backup", targets.get(1).name);
            assertEquals("https://backup.example.invalid/", targets.get(1).url);
            assertEquals("backup", targets.get(1).kind);
        } catch (final AacgConfigParser.NoConfigException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void rejectsDynamicAssignments() {
        assertThrows(IllegalArgumentException.class, () -> AacgConfigParser.parse(
                Jsoup.parse("<script>window.appConfig = loadConfig();</script>")));
        assertThrows(IllegalArgumentException.class, () -> AacgConfigParser.parse(
                Jsoup.parse("<script>window.appConfig = {data: encrypted, key: 'k'};"
                        + "</script>")));
        assertThrows(IllegalArgumentException.class, () -> AacgConfigParser.parse(
                Jsoup.parse("<script>window.appConfig = {data: 'a' + 'b', key: 'k'};"
                        + "</script>")));
    }

    @Test
    void reportsMissingConfig() {
        assertThrows(AacgConfigParser.NoConfigException.class, () -> AacgConfigParser.parse(
                Jsoup.parse("<html><body>nothing</body></html>")));
    }

    @Test
    void tokenizerHonoursEscapesAndComments() {
        final List<AacgJsTokenizer.Token> tokens = AacgJsTokenizer.tokenize(
                "document /* stale */ . getElementById('go\\x2dhome') // old\n"
                        + ".addEventListener(\"click\", handler);");
        final String[] expected = {"document", ".", "getElementById", "(", "go-home",
                ")", ".", "addEventListener", "(", "click", ",", "handler", ")"};
        assertEquals(expected.length + 1, tokens.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).text);
        }
        assertTrue(tokens.get(4).quoted);
        assertEquals(";", tokens.get(expected.length).text);
    }

    @Test
    void tokenizerDecodesUnicodeEscapes() {
        assertToken("'\\x41\\xFF\\u96ea\\ud834\\udd1e'", "A\u00ff雪\uD834\uDD1E");
        assertToken("'\\/\\b\\f\\n\\r\\t\\v\\0'", "/\b\f\n\r\t\u000B\0");
        assertToken("'雪\\q'", "雪q");
        assertToken("'a\\\nb\\\r\nc\\\rd'", "abcd");
    }

    private static void assertToken(final String script, final String expected) {
        final List<AacgJsTokenizer.Token> tokens = AacgJsTokenizer.tokenize(script);
        assertEquals(1, tokens.size());
        assertTrue(tokens.get(0).quoted);
        assertEquals(expected, tokens.get(0).text);
    }
}
