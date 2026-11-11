package com.plasmolysismango.missav;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.List;

/** The latest additions kiosk ({@code /new}). */
public final class MissAvKioskExtractor extends KioskExtractor<StreamInfoItem> {
    private static final int MAX_PAGE = 30;

    private final MissAvSettings settings;

    public MissAvKioskExtractor(final StreamingService service,
                                final ListLinkHandler linkHandler,
                                final String kioskId,
                                final MissAvSettings settings) {
        super(service, linkHandler, kioskId);
        this.settings = settings;
    }

    @Override
    public void onFetchPage(final Downloader downloader) {
    }

    @Override
    public String getName() {
        return "Latest";
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        return fetchPage(getUrl());
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return fetchPage(page.getUrl());
    }

    private InfoItemsPage<StreamInfoItem> fetchPage(final String url)
            throws IOException, ExtractionException {
        final String html = MissAvHttp.fetch(getDownloader(), settings, url);
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        for (final MissAvPageParser.Card card : MissAvPageParser.parseCards(html, url)) {
            collector.commit(new MissAvStreamInfoItemExtractor(card,
                    getService().getBaseUrl()));
        }
        final List<StreamInfoItem> items = collector.getItems();
        final int current = MissAvUrls.pageOf(url);
        final Page next = items.isEmpty() || current >= MAX_PAGE ? null
                : new Page(MissAvUrls.pageUrl(url, current + 1));
        return new InfoItemsPage<>(items, next, collector.getErrors());
    }
}
