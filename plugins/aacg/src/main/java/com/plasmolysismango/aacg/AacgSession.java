package com.plasmolysismango.aacg;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** Mirror discovery: entry page, script forwarding, then the encrypted appConfig routes. */
public final class AacgSession {
    private static final int MAX_HOPS = 5;
    private static final int DISCOVER_RETRIES = 2;
    private static final long RETRY_PAUSE_MILLIS = 500L;
    private static final long CACHE_MILLIS = 10L * 60L * 1000L;

    public static final class Site {
        public final String baseUrl;
        public final List<AacgConfigParser.Target> targets;

        Site(final String baseUrl, final List<AacgConfigParser.Target> targets) {
            this.baseUrl = baseUrl;
            this.targets = targets;
        }
    }

    private final Object lock = new Object();
    private Site cached;
    private long cachedAt;

    public Site site(final Downloader downloader, final AacgSettings settings)
            throws IOException, ExtractionException {
        synchronized (lock) {
            final long now = System.currentTimeMillis();
            if (cached != null && now - cachedAt < CACHE_MILLIS) {
                return cached;
            }
            try {
                final Site site = discover(downloader, settings);
                cached = site;
                cachedAt = now;
                settings.setLastBase(site.baseUrl);
                return site;
            } catch (final IOException | ExtractionException e) {
                final String last = settings.lastBase();
                if (!last.isEmpty()) {
                    final Site fallback = new Site(last,
                            Collections.<AacgConfigParser.Target>emptyList());
                    cached = fallback;
                    cachedAt = now;
                    return fallback;
                }
                throw e;
            }
        }
    }

    public Site discover(final Downloader downloader, final AacgSettings settings)
            throws IOException, ExtractionException {
        IOException lastError = null;
        for (int attempt = 0; attempt <= DISCOVER_RETRIES; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(RETRY_PAUSE_MILLIS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while discovering 51吃瓜 mirrors", e);
                }
            }
            try {
                return discoverOnce(downloader, settings);
            } catch (final IOException e) {
                lastError = e;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("51吃瓜 mirror discovery failed");
    }

    private Site discoverOnce(final Downloader downloader, final AacgSettings settings)
            throws IOException, ExtractionException {
        String current = AacgHttp.validate(settings.entryUrl());
        String referer = "";
        int budget = MAX_HOPS;
        while (true) {
            final AacgHttp.Page page = AacgHttp.getPage(downloader, settings, current, referer);
            final Document document = Jsoup.parse(page.html, page.url);
            List<AacgConfigParser.Target> targets = null;
            try {
                targets = AacgConfigParser.parse(document);
            } catch (final AacgConfigParser.NoConfigException e) {
                // No inline configuration: follow the script-linked forwarding anchor.
            } catch (final IllegalArgumentException e) {
                throw new ContentNotAvailableException("51吃瓜 config: " + e.getMessage(), e);
            }
            if (targets != null) {
                if (targets.isEmpty()) {
                    throw new ContentNotAvailableException("51吃瓜 config lists no mirrors");
                }
                for (final AacgConfigParser.Target target : targets) {
                    try {
                        AacgHttp.validate(target.url);
                    } catch (final IOException e) {
                        throw new ContentNotAvailableException("51吃瓜 target " + target.name
                                + " is invalid", e);
                    }
                }
                return new Site(AacgUrls.trimTrailingSlashes(targets.get(0).url), targets);
            }
            final String next;
            try {
                next = AacgForwardingDetector.forwardingUrl(document, page.url);
            } catch (final IllegalArgumentException e) {
                throw new ContentNotAvailableException("51吃瓜 entry: " + e.getMessage(), e);
            }
            budget--;
            if (budget <= 0) {
                throw new IOException("51吃瓜: redirect limit exceeded");
            }
            referer = current;
            current = next;
        }
    }

}
