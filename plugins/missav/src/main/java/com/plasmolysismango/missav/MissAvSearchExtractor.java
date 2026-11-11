package com.plasmolysismango.missav;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** MissAV search: Recombee for the first page, HTML search as fallback and for paging. */
public final class MissAvSearchExtractor extends SearchExtractor {
    private static final int RECOMBEE_COUNT = 36;
    private static final int MAX_PAGE = 30;

    private final MissAvSettings settings;

    public MissAvSearchExtractor(final MissAvService service,
                                 final SearchQueryHandler linkHandler,
                                 final MissAvSettings settings) {
        super(service, linkHandler);
        this.settings = settings;
    }

    @Override
    public void onFetchPage(final Downloader downloader) {
    }

    @Override
    protected InfoItemsPage<InfoItem> getInitialPageInternal()
            throws IOException, ExtractionException {
        final String query = getSearchString().trim();
        if (query.isEmpty()) {
            return InfoItemsPage.emptyPage();
        }
        try {
            final List<StreamInfoItem> items = MissAvRecombeeClient.search(
                    getDownloader(), getService(), settings, query, RECOMBEE_COUNT);
            if (!items.isEmpty()) {
                final Page next = items.size() >= RECOMBEE_COUNT
                        ? new Page(MissAvUrls.searchUrl(settings.baseUrl(), query, 2)) : null;
                return new InfoItemsPage<>(new ArrayList<InfoItem>(items), next,
                        new ArrayList<Throwable>());
            }
        } catch (final IOException | ExtractionException | RuntimeException e) {
            // Recombee is a convenience path; fall back to the HTML search page.
        }
        return fetchPage(MissAvUrls.searchUrl(settings.baseUrl(), query, 1));
    }

    @Override
    protected InfoItemsPage<InfoItem> getPageInternal(final Page page)
            throws IOException, ExtractionException {
        return fetchPage(page.getUrl());
    }

    private InfoItemsPage<InfoItem> fetchPage(final String url)
            throws IOException, ExtractionException {
        final String html = MissAvHttp.fetch(getDownloader(), settings, url);
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        for (final MissAvPageParser.Card card : MissAvPageParser.parseCards(html, url)) {
            collector.commit(new MissAvStreamInfoItemExtractor(card,
                    getService().getBaseUrl()));
        }
        final List<InfoItem> items = new ArrayList<>(collector.getItems());
        final int current = MissAvUrls.pageOf(url);
        final Page next = items.isEmpty() || current >= MAX_PAGE ? null
                : new Page(MissAvUrls.pageUrl(url, current + 1));
        return new InfoItemsPage<>(items, next, collector.getErrors());
    }
}
