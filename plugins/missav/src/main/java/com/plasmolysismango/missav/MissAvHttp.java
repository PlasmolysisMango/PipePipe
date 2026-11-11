package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Urls;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.AntiBotException;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** HTTP access with MissAV headers, Cloudflare detection and mirror fallback. */
public final class MissAvHttp {
    private MissAvHttp() {
    }

    public static final class HttpStatusException extends IOException {
        public final int status;

        HttpStatusException(final int status, final String url) {
            super("MissAV returned HTTP " + status + " for " + url);
            this.status = status;
        }
    }

    public static final class DetailPage {
        public final String html;
        public final String url;
        public final String kind;

        DetailPage(final String html, final String url, final String kind) {
            this.html = html;
            this.url = url;
            this.kind = kind;
        }
    }

    public static Response get(final Downloader downloader, final Map<String, String> headers,
                               final String url) throws IOException, ExtractionException {
        final Response response = downloader.get(url, toMultiMap(headers));
        final int code = response.responseCode();
        if (code < 200 || code >= 300) {
            throw new HttpStatusException(code, url);
        }
        return response;
    }

    public static String fetch(final Downloader downloader, final MissAvSettings settings,
                               final String url) throws IOException, ExtractionException {
        String html;
        try {
            html = get(downloader, settings.requestHeaders(), url).responseBody();
            if (isChallengePage(html)) {
                html = fetchFromFallback(downloader, settings, url);
            }
        } catch (final HttpStatusException e) {
            if (e.status == 403) {
                html = fetchFromFallback(downloader, settings, url);
            } else {
                throw e;
            }
        }
        if (isChallengePage(html)) {
            throw new AntiBotException("MissAV returned a Cloudflare challenge. "
                    + "Set the login cookie in the plugin settings or change base_url.");
        }
        return html;
    }

    private static String fetchFromFallback(final Downloader downloader,
                                            final MissAvSettings settings,
                                            final String url) throws IOException,
            ExtractionException {
        final String host = Urls.host(url);
        HttpStatusException last = null;
        for (final String fallback : MissAvUrls.FALLBACK_DOMAINS) {
            if (host != null && fallback.equalsIgnoreCase(host)) {
                continue;
            }
            final String fallbackUrl = MissAvUrls.replaceHost(url, fallback);
            if (fallbackUrl.equals(url)) {
                continue;
            }
            final Response response;
            try {
                response = get(downloader, settings.requestHeaders("https://" + fallback),
                        fallbackUrl);
            } catch (final HttpStatusException e) {
                last = e;
                continue;
            }
            final String body = response.responseBody();
            if (!isChallengePage(body)) {
                return body;
            }
            if (last == null) {
                last = new HttpStatusException(403, fallbackUrl);
            }
        }
        if (last != null) {
            throw last;
        }
        throw new HttpStatusException(403, url);
    }

    /**
     * Fetches the first usable variant page for a code. A missing uuid means the page is not a
     * playable detail page, so the next variant is tried; a 403 is remembered and reported if
     * nothing works, to distinguish blocking from a missing title.
     */
    public static DetailPage fetchDetailPage(final Downloader downloader,
                                             final MissAvSettings settings,
                                             final String code) throws IOException,
            ExtractionException {
        String blocked = null;
        IOException lastError = null;
        final List<MissAvUrls.VariantPage> candidates =
                MissAvUrls.detailCandidates(settings.baseUrl(), code, settings.variant());
        for (final MissAvUrls.VariantPage candidate : candidates) {
            try {
                final String html = fetch(downloader, settings, candidate.url);
                if (MissAvUrls.extractUuid(html) == null) {
                    continue;
                }
                return new DetailPage(html, candidate.url, candidate.kind);
            } catch (final AntiBotException e) {
                blocked = e.getMessage();
            } catch (final HttpStatusException e) {
                lastError = e;
                if (e.status == 403) {
                    blocked = e.getMessage();
                }
            } catch (final IOException e) {
                lastError = e;
            }
        }
        if (blocked != null) {
            throw new AntiBotException(blocked);
        }
        throw new ContentNotAvailableException("MissAV: no playable page for " + code,
                lastError);
    }

    public static boolean isChallengePage(final String html) {
        if (html == null || html.isEmpty()) {
            return false;
        }
        final String lower = html.toLowerCase(Locale.ROOT);
        return lower.contains("just a moment")
                || lower.contains("cf-chl-")
                || lower.contains("checking your browser")
                || lower.contains("verify you are human");
    }

    static Map<String, List<String>> toMultiMap(final Map<String, String> headers) {
        final Map<String, List<String>> out = new java.util.LinkedHashMap<>();
        if (headers != null) {
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                out.put(header.getKey(), new ArrayList<>(Collections.singletonList(
                        header.getValue())));
            }
        }
        return out;
    }
}
