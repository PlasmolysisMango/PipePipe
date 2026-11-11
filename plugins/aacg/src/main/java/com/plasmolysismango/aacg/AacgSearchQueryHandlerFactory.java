package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;

/** Maps a search term to {@code /search/<term>/}. */
public final class AacgSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    private final AacgSettings settings;

    public AacgSearchQueryHandlerFactory(final AacgSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getUrl(final String query, final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        final String term = query == null ? "" : query.trim().replace('/', ' ');
        if (term.isEmpty() || ".".equals(term) || "..".equals(term)) {
            throw new ParsingException("Invalid 51吃瓜 search term: " + query);
        }
        return AacgUrls.trimTrailingSlashes(settings.entryUrl()) + "/search/"
                + AacgUrls.encodeSegment(term) + "/";
    }

    @Override
    public String getSearchString(final String url) {
        try {
            final List<String> segments = AacgUrls.pathSegments(url);
            if (segments.size() >= 2 && "search".equals(segments.get(0))) {
                return AacgUrls.decodeSegment(segments.get(1));
            }
        } catch (final IllegalArgumentException e) {
            return "";
        }
        return "";
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        if (url == null) {
            return false;
        }
        try {
            final List<String> segments = AacgUrls.pathSegments(url);
            if (segments.size() < 2 || segments.size() > 3
                    || !"search".equals(segments.get(0))) {
                return false;
            }
            if (segments.size() == 3 && !AacgUrls.isAllDigits(segments.get(2))) {
                return false;
            }
            return !segments.get(1).isEmpty();
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
}
