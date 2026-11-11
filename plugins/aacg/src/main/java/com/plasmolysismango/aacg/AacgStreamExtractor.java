package com.plasmolysismango.aacg;


import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/** Article detail and DPlayer stream extraction. */
public final class AacgStreamExtractor extends StreamExtractor {
    private final AacgSettings settings;
    private final AacgSession session;
    private AacgArticleParser.Article article;
    private String baseUrl = "";

    public AacgStreamExtractor(final StreamingService service,
                               final LinkHandler linkHandler,
                               final AacgSettings settings,
                               final AacgSession session) {
        super(service, linkHandler);
        this.settings = settings;
        this.session = session;
    }

    @Override
    public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        final AacgSession.Site site = session.site(downloader, settings);
        baseUrl = site.baseUrl;
        final String url = getUrl();
        final AacgHttp.Page page;
        try {
            page = AacgHttp.getPage(downloader, settings, url, baseUrl + "/");
        } catch (final IOException e) {
            final String rebased = rebase(url, baseUrl);
            if (rebased == null || rebased.equals(url)) {
                throw e;
            }
            final AacgHttp.Page retried = AacgHttp.getPage(downloader, settings, rebased,
                    baseUrl + "/");
            article = parse(retried);
            return;
        }
        article = parse(page);
    }

    private AacgArticleParser.Article parse(final AacgHttp.Page page) throws ParsingException {
        try {
            return AacgArticleParser.parse(page.html, page.url);
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("51吃瓜 article: " + e.getMessage(), e);
        }
    }

    @Nullable
    private static String rebase(final String url, final String baseUrl) {
        try {
            final String path = AacgUrls.path(url);
            if (path.startsWith("/archives/")) {
                return AacgUrls.trimTrailingSlashes(baseUrl) + path;
            }
        } catch (final IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    @Override
    public String getId() {
        return getLinkHandler().getId();
    }

    @Override
    public String getName() throws ParsingException {
        return requireArticle().title;
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        final String cover = requireArticle().coverUrl;
        return cover.isEmpty() ? "" : AacgUrls.withFragment(cover, "aacg");
    }

    @Override
    public Description getDescription() throws ParsingException {
        final String summary = requireArticle().summary;
        return summary.isEmpty() ? Description.EMPTY_DESCRIPTION
                : new Description(summary, Description.PLAIN_TEXT);
    }

    @Override
    public int getAgeLimit() {
        return 18;
    }

    @Override
    public long getLength() {
        return -1;
    }

    @Override
    public long getViewCount() {
        return -1;
    }

    @Override
    public String getUploaderUrl() {
        return baseUrl;
    }

    @Override
    public String getUploaderName() {
        return AacgService.NAME;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        final String date = requireArticle().publishedAt;
        return date.isEmpty() ? null : date;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return AacgDates.wrap(requireArticle().publishedAt);
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public List<AudioStream> getAudioStreams() {
        return Collections.emptyList();
    }

    @Override
    public List<VideoStream> getVideoStreams() throws ParsingException {
        final List<AacgArticleParser.VideoLink> links = requireArticle().videos;
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        final AacgArticleParser.VideoLink link = links.get(0);
        final DeliveryMethod delivery = isHls(link) ? DeliveryMethod.HLS
                : DeliveryMethod.PROGRESSIVE_HTTP;
        final VideoStream stream = new VideoStream.Builder()
                .setId("h264-0")
                .setContent(link.url, true)
                .setMediaFormat(MediaFormat.MPEG_4)
                .setDeliveryMethod(delivery)
                .setResolution("")
                .setIsVideoOnly(false)
                .setRequestHeaders(settings.pageHeaders(baseUrl + "/"))
                .setCacheable(false)
                .build();
        final List<VideoStream> streams = new ArrayList<>();
        streams.add(stream);
        return streams;
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() {
        return Collections.emptyList();
    }

    private static boolean isHls(final AacgArticleParser.VideoLink link) {
        if ("hls".equalsIgnoreCase(link.type.trim())) {
            return true;
        }
        final String path = link.url.toLowerCase(java.util.Locale.ROOT);
        final int query = path.indexOf('?');
        final String clean = query < 0 ? path : path.substring(0, query);
        return clean.endsWith(".m3u8");
    }

    private AacgArticleParser.Article requireArticle() throws ParsingException {
        if (article == null) {
            throw new ParsingException("51吃瓜 article page was not fetched");
        }
        return article;
    }
}
