package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;

import java.util.regex.Pattern;

/** Accepts {@code /archives/<id>/} article urls. */
public final class AacgLinkHandlerFactory extends LinkHandlerFactory {
    private static final Pattern ARTICLE_PATH = Pattern.compile("^/archives/(\\d+)/?$");

    private final AacgSettings settings;

    public AacgLinkHandlerFactory(final AacgSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        if (url == null) {
            throw new ParsingException("URL is null");
        }
        final String path;
        try {
            path = AacgUrls.path(url);
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("Not a 51吃瓜 article URL: " + url, e);
        }
        final java.util.regex.Matcher matcher = ARTICLE_PATH.matcher(path);
        if (!matcher.matches()) {
            throw new ParsingException("Not a 51吃瓜 article URL: " + url);
        }
        return matcher.group(1);
    }

    @Override
    public String getUrl(final String id) {
        return AacgSession.articleUrl(settings, id);
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        if (url == null) {
            return false;
        }
        try {
            return ARTICLE_PATH.matcher(AacgUrls.path(url)).matches();
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
}
