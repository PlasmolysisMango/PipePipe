package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.List;

/** The homepage feed ({@code /}, {@code /page/N/}). */
public final class AacgHomeKioskExtractor extends KioskExtractor<StreamInfoItem> {
    private final AacgSettings settings;
    private final AacgSession session;

    public AacgHomeKioskExtractor(final StreamingService service,
                                  final ListLinkHandler linkHandler,
                                  final String kioskId,
                                  final AacgSettings settings,
                                  final AacgSession session) {
        super(service, linkHandler, kioskId);
        this.settings = settings;
        this.session = session;
    }

    @Override
    public void onFetchPage(final Downloader downloader) {
    }

    @Override
    public String getName() {
        return "Home";
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final AacgSession.Site site = session.site(getDownloader(), settings);
        return fetchPage(site.baseUrl + "/");
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return fetchPage(page.getUrl());
    }

    private InfoItemsPage<StreamInfoItem> fetchPage(final String url)
            throws IOException, ExtractionException {
        final AacgHttp.Page page = AacgHttp.getPage(getDownloader(), settings, url,
                AacgUrls.origin(url) + "/");
        final AacgListParser.ListPage parsed;
        try {
            parsed = AacgListParser.parse(page.html, page.url);
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("51吃瓜 feed: " + e.getMessage(), e);
        }
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        for (final AacgListParser.Card card : parsed.items) {
            collector.commit(new AacgStreamInfoItemExtractor(card,
                    getService().getBaseUrl()));
        }
        final List<StreamInfoItem> items = collector.getItems();
        return new InfoItemsPage<>(items, nextPage(url, parsed, items), collector.getErrors());
    }

    private static Page nextPage(final String url, final AacgListParser.ListPage parsed,
                                 final List<StreamInfoItem> items) {
        if (items.isEmpty()) {
            return null;
        }
        if (parsed.pagination.nextUrl != null && !parsed.pagination.nextUrl.isEmpty()) {
            return new Page(parsed.pagination.nextUrl);
        }
        if (parsed.pagination.total <= parsed.pagination.current) {
            return null;
        }
        final int scheme = url.indexOf("://");
        final int pathStart = url.indexOf('/', scheme < 0 ? 0 : scheme + 3);
        final String origin = pathStart < 0 ? url : url.substring(0, pathStart);
        return new Page(origin + "/page/" + (parsed.pagination.current + 1) + "/");
    }
}
