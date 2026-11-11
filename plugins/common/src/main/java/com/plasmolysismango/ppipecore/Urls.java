package com.plasmolysismango.ppipecore;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** URL helpers shared by the plugins. */
public final class Urls {
    private Urls() {
    }

    /** Resolves {@code ref} against {@code base}, returning it unchanged on parse errors. */
    public static String resolve(final String base, final String ref) {
        if (ref == null || ref.isEmpty()) {
            return "";
        }
        if (isAbsolute(ref)) {
            return ref;
        }
        try {
            return new URI(base).resolve(ref).toString();
        } catch (final URISyntaxException | IllegalArgumentException e) {
            return ref;
        }
    }

    public static boolean isAbsolute(final String url) {
        return url.startsWith("https://") || url.startsWith("http://");
    }

    /** @return scheme://host[:port] of the url, or the url without trailing slashes */
    public static String origin(final String url) {
        try {
            final URI uri = new URI(url);
            final String scheme = uri.getScheme();
            final String host = uri.getHost();
            if (scheme == null || host == null) {
                return trimTrailingSlashes(url);
            }
            final int port = uri.getPort();
            return scheme + "://" + host + (port >= 0 ? ":" + port : "");
        } catch (final URISyntaxException e) {
            return trimTrailingSlashes(url);
        }
    }

    public static String host(final String url) {
        try {
            return new URI(url).getHost();
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    public static String path(final String url) {
        try {
            return new URI(url).getPath();
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    public static String stripFragment(final String url) {
        final int index = url.indexOf('#');
        return index < 0 ? url : url.substring(0, index);
    }

    /** Matches the whole fragment, so urls normalized by the host (trailing '/') still match. */
    public static boolean hasFragment(final String url, final String fragment) {
        if (url == null) {
            return false;
        }
        final int index = url.indexOf('#');
        return index >= 0 && fragment.equals(url.substring(index + 1));
    }

    public static String withFragment(final String url, final String fragment) {
        return stripFragment(url) + "#" + fragment;
    }

    public static String trimTrailingSlashes(final String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }

    public static String normalizeBase(final String url) {
        final String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimTrailingSlashes(trimmed);
    }

    /** Joins a base url and path segments, encoding each segment. */
    public static String joinPath(final String base, final String... segments) {
        final StringBuilder sb = new StringBuilder(trimTrailingSlashes(base));
        for (final String segment : segments) {
            if (segment == null || segment.isEmpty()) {
                continue;
            }
            sb.append('/').append(encodeSegment(segment));
        }
        return sb.toString();
    }

    /** Percent-encodes a path segment, keeping spaces as %20 and slashes encoded. */
    public static String encodeSegment(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** @return the last non-empty path segment, ignoring query and fragment */
    public static String lastPathSegment(final String url) {
        if (url == null) {
            return "";
        }
        String value = url;
        final int cut = indexOfAny(value, "?#");
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        value = trimTrailingSlashes(value);
        final int slash = value.lastIndexOf('/');
        return slash < 0 ? value : value.substring(slash + 1);
    }

    private static int indexOfAny(final String value, final String chars) {
        for (int i = 0; i < value.length(); i++) {
            if (chars.indexOf(value.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }
}
