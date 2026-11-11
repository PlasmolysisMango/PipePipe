package com.plasmolysismango.aacg;

import com.plasmolysismango.ppipecore.ImageMagic;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.plugin.PluginContext;
import org.schabi.newpipe.extractor.plugin.PluginImage;
import org.schabi.newpipe.extractor.plugin.PluginImageProvider;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** 51吃瓜 service definition and decrypted-cover provider. */
public final class AacgService extends StreamingService implements PluginImageProvider {
    public static final String NAME = "51吃瓜";
    private static final long INFO_CACHE_MILLIS = 5L * 60L * 1000L;
    private static final int IMAGE_MAX_AGE_SECONDS = 3600;

    private final AacgSettings settings;
    private final AacgSession session = new AacgSession();
    private final AacgImageCache imageCache = new AacgImageCache();
    private final AacgLinkHandlerFactory streamLinkHandlerFactory;
    private final AacgSearchQueryHandlerFactory searchQueryHandlerFactory;
    private final AacgKioskLinkHandlerFactory kioskLinkHandlerFactory;

    public AacgService(final int id, final PluginContext context) {
        super(id, NAME, Collections.singletonList(ServiceInfo.MediaCapability.VIDEO));
        settings = new AacgSettings(context.getPreferences());
        streamLinkHandlerFactory = new AacgLinkHandlerFactory(settings);
        searchQueryHandlerFactory = new AacgSearchQueryHandlerFactory(settings);
        kioskLinkHandlerFactory = new AacgKioskLinkHandlerFactory(settings);
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
    public java.util.Map<String, String> getImageRequestHeaders(final String imageUrl) {
        return null;
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
        return new AacgSearchExtractor(this, queryHandler, settings, session);
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        return new AacgStreamExtractor(this, linkHandler, settings, session);
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList kioskList = new KioskList(this);
        try {
            kioskList.addKioskEntry(
                    (service, url, kioskId) -> new AacgHomeKioskExtractor(service,
                            kioskLinkHandlerFactory.fromId(kioskId), kioskId, settings,
                            session),
                    kioskLinkHandlerFactory, AacgKioskLinkHandlerFactory.KIOSK_ID);
        } catch (final Exception e) {
            throw new ExtractionException("Could not register 51吃瓜 kiosk", e);
        }
        kioskList.setDefaultKiosk(AacgKioskLinkHandlerFactory.KIOSK_ID);
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

    @Override
    public boolean handlesImage(@Nonnull final String imageUrl) {
        try {
            return settings.decryptImages() && AacgUrls.hasFragment(imageUrl, "aacg");
        } catch (final RuntimeException e) {
            return false;
        }
    }

    @Nonnull
    @Override
    public PluginImage loadImage(@Nonnull final String imageUrl) throws IOException {
        if (!handlesImage(imageUrl)) {
            throw new IOException("51吃瓜 does not handle this image");
        }
        final String url = AacgUrls.stripFragment(imageUrl);
        final PluginImage cached = imageCache.get(url);
        if (cached != null) {
            return cached;
        }
        final Downloader downloader = NewPipe.getDownloader();
        final byte[] raw;
        try {
            raw = AacgHttp.getImage(downloader, settings, url);
        } catch (final ReCaptchaException e) {
            throw new IOException("51吃瓜: cover image requires a captcha", e);
        }
        final byte[] decoded;
        try {
            decoded = AacgImageCipher.decode(raw);
        } catch (final GeneralSecurityException e) {
            throw new IOException("51吃瓜: could not decrypt cover image", e);
        }
        final String mediaType = ImageMagic.detect(decoded);
        if (mediaType == null) {
            throw new IOException("51吃瓜: decoded cover is not a recognized image");
        }
        final PluginImage image = new PluginImage(decoded, mediaType, IMAGE_MAX_AGE_SECONDS);
        imageCache.put(url, image);
        return image;
    }
}
