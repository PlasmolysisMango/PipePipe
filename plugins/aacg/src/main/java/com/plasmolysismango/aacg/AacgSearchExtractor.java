package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Site search over {@code /search/<term>/[<page>/]}. */
public final class AacgSearchExtractor extends SearchExtractor {
    private final AacgSettings settings;
    private final AacgSession session;

    public AacgSearchExtractor(final AacgService service,
                               final SearchQueryHandler linkHandler,
                               final AacgSettings settings,
                               final AacgSession session) {
        super(service, linkHandler);
        this.settings = settings;
        this.session = session;
    }

    @Override
    public void onFetchPage(final Downloader downloader) {
    }

    @Override
    protected InfoItemsPage<InfoItem> getInitialPageInternal()
            throws IOException, ExtractionException {
        final String term = getSearchString().trim();
        if (term.isEmpty()) {
            return InfoItemsPage.emptyPage();
        }
        final AacgSession.Site site = session.site(getDownloader(), settings);
        final String url = site.baseUrl + "/search/" + AacgUrls.encodeSegment(term) + "/";
        return fetchPage(url);
    }

    @Override
    protected InfoItemsPage<InfoItem> getPageInternal(final Page page)
            throws IOException, ExtractionException {
        return fetchPage(page.getUrl());
    }

    private InfoItemsPage<InfoItem> fetchPage(final String url)
            throws IOException, ExtractionException {
        final AacgHttp.Page page = AacgHttp.getPage(getDownloader(), settings, url,
                AacgUrls.origin(url) + "/");
        final AacgListParser.ListPage parsed;
        try {
            parsed = AacgListParser.parse(page.html, page.url);
        } catch (final IllegalArgumentException e) {
            throw new org.schabi.newpipe.extractor.exceptions.ParsingException(
                    "51吃瓜 search: " + e.getMessage(), e);
        }
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        for (final AacgListParser.Card card : parsed.items) {
            collector.commit(new AacgStreamInfoItemExtractor(card,
                    getService().getBaseUrl()));
        }
        final List<InfoItem> items = new ArrayList<>(collector.getItems());
        return new InfoItemsPage<>(items, nextPage(url, parsed, items), collector.getErrors());
    }

    private static Page nextPage(final String url, final AacgListParser.ListPage parsed,
                                 final List<InfoItem> items) {
        if (items.isEmpty()) {
            return null;
        }
        if (parsed.pagination.nextUrl != null && !parsed.pagination.nextUrl.isEmpty()) {
            return new Page(parsed.pagination.nextUrl);
        }
        if (parsed.pagination.total <= parsed.pagination.current) {
            return null;
        }
        final List<String> segments = AacgUrls.pathSegments(url);
        final String term = segments.size() >= 2 ? segments.get(1) : "";
        // A plain-number term would make the next page collide with the article-style
        // /search/<term>/<page>/ ambiguity, so paging falls back to pagination links only.
        if (term.isEmpty() || AacgUrls.isAllDigits(term)) {
            return null;
        }
        final int scheme = url.indexOf("://");
        final int pathStart = url.indexOf('/', scheme < 0 ? 0 : scheme + 3);
        final String origin = pathStart < 0 ? url : url.substring(0, pathStart);
        return new Page(origin + "/search/" + term + "/"
                + (parsed.pagination.current + 1) + "/");
    }
}
