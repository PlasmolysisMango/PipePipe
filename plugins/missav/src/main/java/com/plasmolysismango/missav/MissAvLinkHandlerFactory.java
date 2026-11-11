package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Urls;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;

import java.util.Locale;

/** Accepts MissAV detail urls and maps them to their code. */
public final class MissAvLinkHandlerFactory extends LinkHandlerFactory {
    private static final String[] VARIANT_SUFFIXES = {
            "-chinese-subtitle", "-uncensored-leak"
    };
    private final MissAvSettings settings;

    public MissAvLinkHandlerFactory(final MissAvSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        String segment = Urls.lastPathSegment(url);
        final String lower = segment.toLowerCase(Locale.ROOT);
        for (final String suffix : VARIANT_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                segment = segment.substring(0, segment.length() - suffix.length());
                break;
            }
        }
        final String code = MissAvUrls.extractCode(segment);
        if (code.isEmpty()) {
            throw new ParsingException("Not a MissAV code: " + url);
        }
        return code;
    }

    @Override
    public String getUrl(final String id) {
        return getUrl(id, settings.baseUrl());
    }

    @Override
    public String getUrl(final String id, final String baseUrl) {
        return Urls.joinPath(baseUrl, "cn", MissAvUrls.slug(id));
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        if (url == null || !Urls.isAbsolute(url)) {
            return false;
        }
        final String path = Urls.path(url);
        return path != null && path.matches("(?:/dm\\d+)?/cn/[^/]+/?");
    }
}
