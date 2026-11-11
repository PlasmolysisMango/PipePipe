package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;

/** Maps the single "Home" kiosk to {@code /} and its pages. */
public final class AacgKioskLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String KIOSK_ID = "Home";

    private final AacgSettings settings;

    public AacgKioskLinkHandlerFactory(final AacgSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        if (!onAcceptUrl(url)) {
            throw new ParsingException("Not a 51吃瓜 homepage URL: " + url);
        }
        return KIOSK_ID;
    }

    @Override
    public String getUrl(final String id, final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) {
        return AacgUrls.trimTrailingSlashes(settings.baseUrl()) + "/";
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        if (url == null) {
            return false;
        }
        try {
            final List<String> segments = AacgUrls.pathSegments(url);
            if (segments.isEmpty()) {
                return true;
            }
            return segments.size() == 2 && "page".equals(segments.get(0))
                    && AacgUrls.isAllDigits(segments.get(1));
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
}
