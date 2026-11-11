package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.AntiBotException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Page and image requests with AACG headers, size limits and challenge detection. */
public final class AacgHttp {
    public static final int MAX_PAGE_BYTES = 2 << 20;
    public static final int MAX_IMAGE_BYTES = 8 << 20;

    private static final String[] CHALLENGE_MARKERS = {
            "just a moment", "cf-chl-", "verify you are human", "checking your browser"
    };

    private AacgHttp() {
    }

    public static final class HttpStatusException extends IOException {
        public final int status;

        HttpStatusException(final int status, final String url) {
            super("51吃瓜 returned HTTP " + status + " for " + url);
            this.status = status;
        }
    }

    public static final class Page {
        public final String html;
        public final String url;

        Page(final String html, final String url) {
            this.html = html;
            this.url = url;
        }
    }

    public static Page getPage(final Downloader downloader, final AacgSettings settings,
                               final String url, final String referer)
            throws IOException, ExtractionException {
        final String validated = validate(url);
        final Response response = downloader.get(validated,
                toMultiMap(settings.pageHeaders(referer)));
        final byte[] raw = bodyBytes(response);
        checkStatus(response, validated);
        if (raw.length > MAX_PAGE_BYTES) {
            throw new IOException("51吃瓜: response exceeds " + MAX_PAGE_BYTES + " bytes");
        }
        checkContentType(response);
        final String html = new String(raw, StandardCharsets.UTF_8);
        if (isChallengePage(html)) {
            throw new AntiBotException("51吃瓜 returned a browser challenge page.");
        }
        final String latest = response.latestUrl();
        return new Page(html, latest == null || latest.isEmpty() ? validated : latest);
    }

    public static byte[] getImage(final Downloader downloader, final AacgSettings settings,
                                  final String url)
            throws IOException, ReCaptchaException {
        final String validated = validate(url);
        final Response response = downloader.get(validated,
                toMultiMap(settings.imageHeaders()));
        final byte[] raw = bodyBytes(response);
        checkStatus(response, validated);
        if (raw.length > MAX_IMAGE_BYTES) {
            throw new IOException("51吃瓜: image exceeds " + MAX_IMAGE_BYTES + " bytes");
        }
        return raw;
    }

    public static String validate(final String url) throws IOException {
        try {
            return AacgUrls.normalize(url);
        } catch (final IllegalArgumentException e) {
            throw new IOException("51吃瓜: invalid URL: " + e.getMessage(), e);
        }
    }

    public static boolean isChallengePage(final String html) {
        if (html == null || html.isEmpty()) {
            return false;
        }
        final String lower = html.toLowerCase(Locale.ROOT);
        for (final String marker : CHALLENGE_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    static byte[] bodyBytes(final Response response) {
        final byte[] raw = response.rawResponseBody();
        if (raw != null) {
            return raw;
        }
        return response.responseBody().getBytes(StandardCharsets.UTF_8);
    }

    private static void checkStatus(final Response response, final String url)
            throws HttpStatusException {
        final int code = response.responseCode();
        if (code < 200 || code >= 300) {
            throw new HttpStatusException(code, url);
        }
    }

    private static void checkContentType(final Response response) throws IOException {
        final String header = response.getHeader("Content-Type");
        if (header == null || header.trim().isEmpty()) {
            return;
        }
        final String type = header.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!"text/html".equals(type) && !"application/xhtml+xml".equals(type)) {
            throw new IOException("51吃瓜: expected HTML, got " + header);
        }
    }

    static Map<String, List<String>> toMultiMap(final Map<String, String> headers) {
        final Map<String, List<String>> out = new LinkedHashMap<>();
        for (final Map.Entry<String, String> header : headers.entrySet()) {
            out.put(header.getKey(), new ArrayList<>(
                    Collections.singletonList(header.getValue())));
        }
        return out;
    }
}
