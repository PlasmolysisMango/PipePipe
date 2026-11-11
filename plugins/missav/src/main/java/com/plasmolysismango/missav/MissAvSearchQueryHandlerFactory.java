package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Urls;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/** Maps search queries to {@code /search/<term>?page=N}. */
public final class MissAvSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    private static final String SEARCH_SEGMENT = "/search/";
    private final MissAvSettings settings;

    public MissAvSearchQueryHandlerFactory(final MissAvSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getUrl(final String query, final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        final String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            throw new ParsingException("MissAV: empty search query");
        }
        return MissAvUrls.searchUrl(settings.baseUrl(), term, 1);
    }

    @Override
    public String getSearchString(final String url) {
        final String path = Urls.path(url == null ? "" : url);
        if (path == null) {
            return "";
        }
        final int index = path.indexOf(SEARCH_SEGMENT);
        if (index < 0) {
            return "";
        }
        String term = path.substring(index + SEARCH_SEGMENT.length());
        while (term.endsWith("/")) {
            term = term.substring(0, term.length() - 1);
        }
        try {
            return URLDecoder.decode(term, "UTF-8");
        } catch (final UnsupportedEncodingException | IllegalArgumentException e) {
            return term;
        }
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        if (url == null || !Urls.isAbsolute(url)) {
            return false;
        }
        final String path = Urls.path(url);
        return path != null && path.contains(SEARCH_SEGMENT);
    }
}
