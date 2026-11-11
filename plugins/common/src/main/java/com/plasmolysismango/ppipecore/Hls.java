package com.plasmolysismango.ppipecore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Minimal HLS master playlist parser. */
public final class Hls {
    private Hls() {
    }

    public static final class Variant {
        public final String url;
        public final long bandwidth;
        public final int width;
        public final int height;
        public final String codecs;
        public final String frameRate;

        Variant(final String url, final long bandwidth, final int width, final int height,
                final String codecs, final String frameRate) {
            this.url = url;
            this.bandwidth = bandwidth;
            this.width = width;
            this.height = height;
            this.codecs = codecs;
            this.frameRate = frameRate;
        }
    }

    public static boolean isMaster(final String content) {
        return content != null && content.contains("#EXT-X-STREAM-INF:");
    }

    /**
     * @return the variants of a master playlist sorted by ascending bandwidth, or an empty list
     * for a media playlist
     */
    public static List<Variant> parseMaster(final String content, final String playlistUrl) {
        if (content == null) {
            return Collections.emptyList();
        }
        final List<Variant> out = new ArrayList<>();
        String pendingAttributes = null;
        int pendingWidth = 0;
        int pendingHeight = 0;
        long pendingBandwidth = 0;
        String pendingCodecs = null;
        String pendingFrameRate = null;
        for (final String rawLine : content.split("\\r?\\n")) {
            final String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                pendingAttributes = line.substring("#EXT-X-STREAM-INF:".length());
                pendingBandwidth = parseLong(attribute(pendingAttributes, "BANDWIDTH"), 0);
                pendingCodecs = attribute(pendingAttributes, "CODECS");
                pendingFrameRate = attribute(pendingAttributes, "FRAME-RATE");
                pendingWidth = 0;
                pendingHeight = 0;
                final String resolution = attribute(pendingAttributes, "RESOLUTION");
                if (resolution != null) {
                    final int x = resolution.indexOf('x');
                    if (x > 0) {
                        pendingWidth = (int) parseLong(resolution.substring(0, x), 0);
                        pendingHeight = (int) parseLong(resolution.substring(x + 1), 0);
                    }
                }
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            if (pendingAttributes != null) {
                out.add(new Variant(Urls.resolve(playlistUrl, line), pendingBandwidth,
                        pendingWidth, pendingHeight, pendingCodecs, pendingFrameRate));
                pendingAttributes = null;
            }
        }
        out.sort(Comparator.comparingLong(v -> v.bandwidth));
        return out;
    }

    static String attribute(final String attributes, final String name) {
        final String needle = name + "=";
        int from = 0;
        while (true) {
            final int index = attributes.indexOf(needle, from);
            if (index < 0) {
                return null;
            }
            if (index > 0 && attributes.charAt(index - 1) != ',') {
                from = index + needle.length();
                continue;
            }
            int start = index + needle.length();
            if (start < attributes.length() && attributes.charAt(start) == '"') {
                final int end = attributes.indexOf('"', start + 1);
                if (end < 0) {
                    return null;
                }
                return attributes.substring(start + 1, end);
            }
            int end = attributes.indexOf(',', start);
            if (end < 0) {
                end = attributes.length();
            }
            return attributes.substring(start, end).trim();
        }
    }

    private static long parseLong(final String value, final long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }
}
