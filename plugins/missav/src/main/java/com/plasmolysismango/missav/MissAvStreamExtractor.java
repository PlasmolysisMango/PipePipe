package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Hls;
import com.plasmolysismango.ppipecore.Urls;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MissAV detail page: metadata and the surrit HLS playlist variants. */
public final class MissAvStreamExtractor extends StreamExtractor {
    private static final int AGE_LIMIT = 18;

    private final MissAvSettings settings;
    private String uuid;
    private MissAvPageParser.Detail detail;
    private List<VideoStream> videoStreams;

    public MissAvStreamExtractor(final StreamingService service,
                                 final LinkHandler linkHandler,
                                 final MissAvSettings settings) {
        super(service, linkHandler);
        this.settings = settings;
    }

    @Override
    public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        final MissAvHttp.DetailPage page = MissAvHttp.fetchDetailPage(
                downloader, settings, getId());
        uuid = MissAvUrls.extractUuid(page.html);
        if (uuid == null) {
            throw new ContentNotAvailableException("MissAV: page has no playable source");
        }
        detail = MissAvPageParser.parseDetail(page.html, page.url);
    }

    @Override
    public String getName() throws ParsingException {
        if (detail != null && !detail.title.isEmpty()) {
            return detail.title;
        }
        return getId();
    }

    @Override
    public String getThumbnailUrl() {
        if (detail == null || detail.thumbnailUrl.isEmpty()) {
            return "";
        }
        return Urls.withFragment(detail.thumbnailUrl, "missav");
    }

    @Override
    public String getUploaderUrl() {
        return settings.baseUrl();
    }

    @Override
    public String getUploaderName() {
        return MissAvService.NAME;
    }

    @Override
    public int getAgeLimit() {
        return AGE_LIMIT;
    }

    @Override
    public long getViewCount() {
        return -1;
    }

    @Override
    public List<String> getTags() {
        return detail == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(detail.tags));
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
    public List<VideoStream> getVideoStreams() throws IOException, ExtractionException {
        assertPageFetched();
        if (videoStreams != null) {
            return videoStreams;
        }
        final String playlistUrl = MissAvUrls.playlistUrl(uuid);
        final String playlist = MissAvHttp.get(getDownloader(), settings.requestHeaders(),
                playlistUrl).responseBody();
        final List<Hls.Variant> variants = Hls.parseMaster(playlist, playlistUrl);
        final Map<String, String> headers = playbackHeaders();
        final List<VideoStream> streams = new ArrayList<>();
        if (variants.isEmpty()) {
            streams.add(buildStream("hls-media", playlistUrl, "", 0, 0, 0, null, headers));
        } else {
            for (final Hls.Variant variant : variants) {
                final String id = variant.height > 0
                        ? "hls-" + variant.height : "hls-" + variant.bandwidth;
                streams.add(buildStream(id, variant.url,
                        variant.height > 0 ? variant.height + "p" : "",
                        variant.width, variant.height, variant.bandwidth,
                        variant.codecs, headers));
            }
        }
        videoStreams = Collections.unmodifiableList(streams);
        return videoStreams;
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() {
        return Collections.emptyList();
    }

    private Map<String, String> playbackHeaders() {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", settings.userAgent());
        headers.put("Referer", settings.baseUrl() + "/");
        final String cookie = settings.cookie();
        if (!cookie.isEmpty()) {
            headers.put("Cookie", cookie);
        }
        return headers;
    }

    private static VideoStream buildStream(final String id, final String url,
                                           final String resolution, final int width,
                                           final int height, final long bandwidth,
                                           final String codecs,
                                           final Map<String, String> headers) {
        final VideoStream.Builder builder = new VideoStream.Builder()
                .setId(id)
                .setContent(url, true)
                .setMediaFormat(MediaFormat.MPEG_4)
                .setDeliveryMethod(DeliveryMethod.HLS)
                .setResolution(resolution)
                .setIsVideoOnly(false)
                .setRequestHeaders(headers)
                .setCacheable(false);
        if (width > 0) {
            builder.setWidth(width);
        }
        if (height > 0) {
            builder.setHeight(height);
        }
        if (bandwidth > 0 && bandwidth <= Integer.MAX_VALUE) {
            builder.setBitrate((int) bandwidth);
        }
        if (codecs != null && !codecs.isEmpty()) {
            builder.setCodec(codecs);
        }
        return builder.build();
    }
}
