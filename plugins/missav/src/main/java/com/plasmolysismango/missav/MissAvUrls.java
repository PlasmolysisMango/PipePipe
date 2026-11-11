package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Urls;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Url, code and playlist helpers ported from the videoviewer MissAV source. */
public final class MissAvUrls {
    public static final String[] FALLBACK_DOMAINS = {"missav.ws", "missav123.com"};
    public static final Pattern CODE_PATTERN =
            Pattern.compile("(?i)([A-Z]+(?:-[A-Z]+)*?)(-?)(\\d+)([A-Za-z]?)");
    public static final Pattern UUID_PATTERN =
            Pattern.compile("m3u8\\|([a-f0-9|]+)\\|com\\|surrit\\|https\\|video");
    private static final Pattern DM_PREFIX = Pattern.compile("(?i)^dm\\d+$");
    private static final Pattern PAGE_PARAM = Pattern.compile("[?&]page=(\\d+)");

    private MissAvUrls() {
    }

    public static String normalizeCode(final String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Pads the trailing digit run of a code to three digits (LAFBD-41 to LAFBD-041).
     * MissAV indexes both slugs and search hits by the padded form.
     */
    public static String padCodeDigits(final String code) {
        final String value = code == null ? "" : code.trim();
        int lastNonDigit = -1;
        for (int i = value.length() - 1; i >= 0; i--) {
            final char c = value.charAt(i);
            if (c < '0' || c > '9') {
                lastNonDigit = i;
                break;
            }
        }
        if (lastNonDigit < 0 || lastNonDigit == value.length() - 1) {
            return value;
        }
        return value.substring(0, lastNonDigit + 1) + padDigits(value.substring(lastNonDigit + 1));
    }

    private static String padDigits(final String value) {
        String digits = value;
        while (digits.length() < 3) {
            digits = "0" + digits;
        }
        return digits;
    }

    public static String slug(final String code) {
        return padCodeDigits(code).toLowerCase(Locale.ROOT);
    }

    public static String extractCode(final String text) {
        if (text == null) {
            return "";
        }
        final Matcher matcher = CODE_PATTERN.matcher(text.toUpperCase(Locale.ROOT));
        if (!matcher.find()) {
            return "";
        }
        // MissAV keys titles by the zero-padded number, so "lafbd-41" is "LAFBD-041".
        final String prefix = matcher.group(1);
        final String separator = matcher.group(2);
        final String digits = padDigits(matcher.group(3));
        final String suffix = matcher.group(4);
        return prefix + separator + digits + suffix;
    }

    public static final class VariantPage {
        public final String url;
        public final String kind;

        VariantPage(final String url, final String kind) {
            this.url = url;
            this.kind = kind;
        }
    }

    /** Detail page candidates in preferred-variant order, mirror last. */
    public static List<VariantPage> detailCandidates(final String baseUrl, final String code,
                                                     final String preferredVariant) {
        final String slug = slug(code);
        final VariantPage cnsub = new VariantPage(
                baseUrl + "/cn/" + slug + "-chinese-subtitle", "cnsub");
        final VariantPage uncensored = new VariantPage(
                baseUrl + "/cn/" + slug + "-uncensored-leak", "uncensored");
        final VariantPage normal = new VariantPage(baseUrl + "/cn/" + slug, "normal");
        final VariantPage mirror = new VariantPage(baseUrl + "/dm13/cn/" + slug, "normal");
        final List<VariantPage> out;
        if ("uncensored".equals(preferredVariant)) {
            out = Arrays.asList(uncensored, cnsub, normal, mirror);
        } else if ("normal".equals(preferredVariant)) {
            out = Arrays.asList(normal, cnsub, uncensored, mirror);
        } else {
            out = Arrays.asList(cnsub, uncensored, normal, mirror);
        }
        return new ArrayList<>(out);
    }

    public static String searchUrl(final String baseUrl, final String query, final int page) {
        final String url = Urls.joinPath(baseUrl, "search", query.trim());
        return page > 1 ? url + "?page=" + page : url;
    }

    public static String latestUrl(final String baseUrl, final int page) {
        return page > 1 ? baseUrl + "/new?page=" + page : baseUrl + "/new";
    }

    /** Increments the page query of a search/latest url; page 1 drops the parameter. */
    public static String pageUrl(final String url, final int page) {
        final String clean = Urls.stripFragment(url);
        final int queryStart = clean.indexOf('?');
        final String base = queryStart < 0 ? clean : clean.substring(0, queryStart);
        final String query = queryStart < 0 ? "" : clean.substring(queryStart + 1);
        final StringBuilder kept = new StringBuilder();
        if (!query.isEmpty()) {
            for (final String pair : query.split("&")) {
                if (pair.isEmpty() || pair.startsWith("page=")) {
                    continue;
                }
                if (kept.length() > 0) {
                    kept.append('&');
                }
                kept.append(pair);
            }
        }
        if (page > 1) {
            if (kept.length() > 0) {
                kept.append('&');
            }
            kept.append("page=").append(page);
        }
        return kept.length() == 0 ? base : base + "?" + kept;
    }

    public static int pageOf(final String url) {
        final Matcher matcher = PAGE_PARAM.matcher(url == null ? "" : url);
        if (!matcher.find()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(matcher.group(1)));
        } catch (final NumberFormatException e) {
            return 1;
        }
    }

    /** Extracts the surrit playlist uuid by reversing the pipe separated segments. */
    public static String extractUuid(final String html) {
        if (html == null) {
            return null;
        }
        final Matcher matcher = UUID_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        final String[] parts = matcher.group(1).split("\\|");
        final StringBuilder out = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            if (out.length() > 0) {
                out.append('-');
            }
            out.append(parts[i]);
        }
        return out.toString();
    }

    public static String playlistUrl(final String uuid) {
        return "https://surrit.com/" + uuid + "/playlist.m3u8";
    }

    public static String replaceHost(final String url, final String newHost) {
        try {
            final URI uri = new URI(url);
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                return url;
            }
            return new URI(uri.getScheme(), null, newHost, uri.getPort(), uri.getRawPath(),
                    uri.getRawQuery(), uri.getRawFragment()).toString();
        } catch (final URISyntaxException e) {
            return url;
        }
    }

    /** Whether a list page link is navigation or a variant suffix rather than a video card. */
    public static boolean isNavHref(final String href) {
        if (href == null || href.contains("/search/")) {
            return true;
        }
        final String segment = Urls.lastPathSegment(href);
        if (segment.isEmpty() || DM_PREFIX.matcher(segment).matches()) {
            return true;
        }
        for (int i = 0; i < segment.length(); i++) {
            if (Character.isDigit(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
