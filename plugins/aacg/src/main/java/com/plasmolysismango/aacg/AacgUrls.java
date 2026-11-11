package com.plasmolysismango.aacg;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Strict URL parsing, resolution and same-origin checks for AACG. */
public final class AacgUrls {
    private AacgUrls() {
    }

    /** Parses an absolute http(s) URL without credentials and rejects private literal hosts. */
    public static URI parse(final String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("URL is missing");
        }
        if (raw.indexOf('\\') >= 0 || hasControl(raw)) {
            throw new IllegalArgumentException("URL contains invalid characters");
        }
        final URI uri;
        try {
            uri = new URI(raw.trim());
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
        final String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("Expected an absolute HTTP(S) URL");
        }
        final String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("URL has no host");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL must not contain credentials");
        }
        if (uri.getRawAuthority() != null && uri.getRawAuthority().contains("%")) {
            throw new IllegalArgumentException("URL host must not be percent-encoded");
        }
        if (uri.getPort() < -1 || uri.getPort() > 65535) {
            throw new IllegalArgumentException("Invalid URL port");
        }
        if (isNonPublicLiteral(host)) {
            throw new IllegalArgumentException("Non-public target");
        }
        final String normalizedHost = host.toLowerCase(Locale.ROOT);
        try {
            return new URI(scheme, uri.getUserInfo(), normalizedHost, uri.getPort(),
                    uri.getRawPath(), uri.getRawQuery(), null);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    /** Adds https when no scheme is present, then validates and normalizes. */
    public static String normalize(final String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://" + value;
        }
        return parse(value).toString();
    }

    public static String resolve(final String base, final String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is missing");
        }
        if (reference.indexOf('\\') >= 0 || hasControl(reference)) {
            throw new IllegalArgumentException("URL contains invalid characters");
        }
        final URI resolved;
        try {
            resolved = new URI(base).resolve(reference.trim());
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
        return parse(resolved.toString()).toString();
    }

    /** Resolves a possibly relative href; returns null when it is empty or invalid. */
    public static String resolveOrNull(final String base, final String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return null;
        }
        try {
            return resolve(base, reference);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean sameOrigin(final String first, final String second) {
        try {
            return origin(first).equals(origin(second));
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    public static String origin(final String url) {
        final URI uri = parse(url);
        final int port = uri.getPort() >= 0 ? uri.getPort() : defaultPort(uri.getScheme());
        return uri.getScheme() + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
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

    public static String path(final String url) {
        return parse(url).getRawPath() == null ? "" : parse(url).getRawPath();
    }

    public static String query(final String url) {
        return parse(url).getRawQuery();
    }

    public static String trimTrailingSlashes(final String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }

    public static List<String> pathSegments(final String url) {
        final String path = path(url);
        final List<String> segments = new ArrayList<>();
        for (final String part : path.split("/")) {
            if (!part.isEmpty()) {
                segments.add(part);
            }
        }
        return segments;
    }

    public static String encodeSegment(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static String decodeSegment(final String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException e) {
            return "";
        }
    }

    public static boolean isAllDigits(final String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static int defaultPort(final String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private static boolean hasControl(final String value) {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNonPublicLiteral(final String host) {
        final String value = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
        final boolean looksLikeIpv4 = value.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
        final boolean looksLikeIpv6 = value.indexOf(':') >= 0;
        if (!looksLikeIpv4 && !looksLikeIpv6) {
            return "localhost".equalsIgnoreCase(value)
                    || value.toLowerCase(Locale.ROOT).endsWith(".localhost");
        }
        try {
            final InetAddress address = InetAddress.getByName(value);
            return address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isMulticastAddress();
        } catch (final Exception e) {
            return true;
        }
    }
}
