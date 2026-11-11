package com.plasmolysismango.missav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.plasmolysismango.ppipecore.test.FakeDownloader;
import com.plasmolysismango.ppipecore.test.FakePluginContext;
import com.plasmolysismango.ppipecore.test.FakePreferences;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MissAvStreamExtractorTest {

    private static final String DETAIL = "<html><head>"
            + "<meta property='og:title' content='SSIS-001 Title'>"
            + "<meta property='og:image' content='https://img.example/cover.jpg'>"
            + "</head><body>"
            + "<script>var x = 'm3u8|aa|bb|cc|com|surrit|https|video';</script>"
            + "</body></html>";

    private static final String PLAYLIST = "#EXTM3U\n"
            + "#EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1280x720,"
            + "CODECS=\"avc1.4d401f,mp4a.40.2\"\n"
            + "720p/index.m3u8\n"
            + "#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360\n"
            + "360p/index.m3u8\n";

    @Test
    void extractsHlsVariantsAndMetadata() throws Exception {
        final FakeDownloader downloader = new FakeDownloader();
        NewPipe.init(downloader, Localization.DEFAULT);
        downloader.respond("https://missav.ws/cn/ssis-001-chinese-subtitle", 200, DETAIL);
        downloader.respond("https://surrit.com/cc-bb-aa/playlist.m3u8", 200, PLAYLIST);

        final Map<String, String> defaults = new HashMap<>();
        defaults.put("base_url", "https://missav.ws");
        defaults.put("variant", "cnsub");
        final FakePreferences preferences = new FakePreferences(defaults);
        final MissAvService service = new MissAvService(1, new FakePluginContext(preferences));
        final LinkHandler linkHandler = service.getStreamLHFactory().fromId("SSIS-001");
        final MissAvStreamExtractor extractor = new MissAvStreamExtractor(service, linkHandler,
                new MissAvSettings(preferences));

        extractor.fetchPage();

        assertEquals("Title", extractor.getName());
        assertEquals("https://img.example/cover.jpg#missav", extractor.getThumbnailUrl());
        assertEquals(18, extractor.getAgeLimit());
        assertTrue(extractor.getTags().isEmpty());

        final List<VideoStream> streams = extractor.getVideoStreams();
        assertEquals(2, streams.size());

        final VideoStream low = streams.get(0);
        assertEquals("hls-360", low.getId());
        assertEquals("360p", low.getResolution());
        assertEquals("https://surrit.com/cc-bb-aa/360p/index.m3u8", low.getContent());
        assertEquals(DeliveryMethod.HLS, low.getDeliveryMethod());
        assertEquals(MediaFormat.MPEG_4, low.getFormat());
        assertEquals("https://missav.ws/", low.getRequestHeaders().get("Referer"));
        assertEquals(Boolean.FALSE, low.getCacheable());

        final VideoStream high = streams.get(1);
        assertEquals("hls-720", high.getId());
        assertEquals(720, high.getHeight());
        assertEquals(2000000, high.getBitrate());
        assertEquals("avc1.4d401f,mp4a.40.2", high.getCodec());
    }
}
