package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Urls;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;

/** Maps the single "Latest" kiosk to {@code /new}. */
public final class MissAvKioskLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String KIOSK_ID = "Latest";
    private final MissAvSettings settings;

    public MissAvKioskLinkHandlerFactory(final MissAvSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        if (!onAcceptUrl(url)) {
            throw new ParsingException("Not a MissAV kiosk url: " + url);
        }
        return KIOSK_ID;
    }

    @Override
    public String getUrl(final String id, final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) {
        return MissAvUrls.latestUrl(settings.baseUrl(), 1);
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        if (url == null || !Urls.isAbsolute(url)) {
            return false;
        }
        final String path = Urls.path(url);
        return "/new".equals(path) || "/new/".equals(path);
    }
}
