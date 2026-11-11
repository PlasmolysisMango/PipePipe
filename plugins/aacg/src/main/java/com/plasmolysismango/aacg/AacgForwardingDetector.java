package com.plasmolysismango.aacg;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finds the single anchor that the entry page's script forwards to. */
final class AacgForwardingDetector {
    private AacgForwardingDetector() {
    }

    static String forwardingUrl(final Document document, final String baseUrl) {
        final Map<String, Boolean> ids = new LinkedHashMap<>();
        final Elements scripts = document.select("script:not([src])");
        for (final Element script : scripts) {
            final List<AacgJsTokenizer.Token> tokens;
            try {
                tokens = AacgJsTokenizer.tokenize(script.data());
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid forwarding script: "
                        + e.getMessage());
            }
            for (int i = 0; i + 5 < tokens.size(); i++) {
                if (!hasTokens(tokens, i, "document", ".", "getElementById", "(")
                        || !tokens.get(i + 4).quoted
                        || !hasTokens(tokens, i + 5, ")")) {
                    continue;
                }
                final boolean direct = hasTokens(tokens, i + 6, ".", "click", "(", ")")
                        || hasTokens(tokens, i - 6, "window", ".", "location", ".",
                        "replace", "(")
                        && hasTokens(tokens, i + 6, ".", "href", ")");
                boolean wrapper = false;
                if (hasTokens(tokens, 0, "(", "function", "(")
                        && tokens.size() > 3 && !tokens.get(3).quoted
                        && hasTokens(tokens, 4, ")", "{")
                        && hasTokens(tokens, i - 3, "}", ")", "(")) {
                    final String parameter = tokens.get(3).text;
                    for (int j = 6; j < i - 3; j++) {
                        wrapper = wrapper
                                || hasTokens(tokens, j, parameter, ".", "click", "(", ")")
                                || hasTokens(tokens, j, "window", ".", "location", ".",
                                "replace", "(", parameter, ".", "href", ")");
                    }
                }
                if (direct || wrapper) {
                    ids.put(tokens.get(i + 4).text, Boolean.TRUE);
                }
            }
        }
        final List<String> links = new ArrayList<>();
        for (final Element anchor : document.select("a[id][href]")) {
            if (ids.containsKey(anchor.id())) {
                links.add(anchor.attr("href"));
            }
        }
        if (links.size() != 1 || links.get(0).trim().isEmpty()) {
            throw new IllegalArgumentException("Expected one script-linked forwarding anchor, "
                    + "found " + links.size());
        }
        try {
            return AacgUrls.resolve(baseUrl, links.get(0));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid forwarding URL", e);
        }
    }

    private static boolean hasTokens(final List<AacgJsTokenizer.Token> tokens, final int start,
                                     final String... words) {
        if (start < 0 || start + words.length > tokens.size()) {
            return false;
        }
        for (int i = 0; i < words.length; i++) {
            final AacgJsTokenizer.Token token = tokens.get(start + i);
            if (token.quoted || !words[i].equals(token.text)) {
                return false;
            }
        }
        return true;
    }
}
