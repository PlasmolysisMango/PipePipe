package com.plasmolysismango.missav;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;
import org.schabi.newpipe.extractor.plugin.PluginContext;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

/** MissAV service definition. */
public final class MissAvService extends StreamingService {
    public static final String NAME = "MissAV";
    private static final long INFO_CACHE_MILLIS = 30L * 60L * 1000L;

    private final MissAvSettings settings;
    private final MissAvLinkHandlerFactory streamLinkHandlerFactory;
    private final MissAvSearchQueryHandlerFactory searchQueryHandlerFactory;
    private final MissAvKioskLinkHandlerFactory kioskLinkHandlerFactory;

    public MissAvService(final int id, final PluginContext context) {
        super(id, NAME, Collections.singletonList(ServiceInfo.MediaCapability.VIDEO));
        settings = new MissAvSettings(context.getPreferences());
        streamLinkHandlerFactory = new MissAvLinkHandlerFactory(settings);
        searchQueryHandlerFactory = new MissAvSearchQueryHandlerFactory(settings);
        kioskLinkHandlerFactory = new MissAvKioskLinkHandlerFactory(settings);
    }

    @Override
    public String getBaseUrl() {
        return settings.baseUrl();
    }

    @Override
    public long getInfoCacheExpirationMillis() {
        return INFO_CACHE_MILLIS;
    }

    @Nullable
    @Override
    public Map<String, String> getImageRequestHeaders(final String imageUrl) {
        return settings.imageHeaders(imageUrl);
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return streamLinkHandlerFactory;
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return searchQueryHandlerFactory;
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        return new MissAvSearchExtractor(this, queryHandler, settings);
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        return new MissAvStreamExtractor(this, linkHandler, settings);
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList kioskList = new KioskList(this);
        try {
            kioskList.addKioskEntry(
                    (service, url, kioskId) -> new MissAvKioskExtractor(service,
                            kioskLinkHandlerFactory.fromId(kioskId), kioskId, settings),
                    kioskLinkHandlerFactory, MissAvKioskLinkHandlerFactory.KIOSK_ID);
        } catch (final Exception e) {
            throw new ExtractionException("Could not register MissAV kiosk", e);
        }
        kioskList.setDefaultKiosk(MissAvKioskLinkHandlerFactory.KIOSK_ID);
        return kioskList;
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return null;
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return null;
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return null;
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return null;
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return null;
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return null;
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler) {
        return null;
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler linkHandler) {
        return null;
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler) {
        return null;
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler) {
        return null;
    }
}
