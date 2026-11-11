package com.plasmolysismango.aacg;

import com.plasmolysismango.ppipecore.test.FakeDownloader;
import com.plasmolysismango.ppipecore.test.FakePluginContext;
import com.plasmolysismango.ppipecore.test.FakePreferences;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AacgStreamExtractorTest {
    private static final String ARTICLE = "<html><head>"
            + "<meta name='description' content='Summary text'></head><body>"
            + "<div id='post' role='main'><article "
            + "itemtype='https://schema.org/BlogPosting'>"
            + "<h1 itemprop='headline'>Article title</h1>"
            + "<meta itemprop='datePublished' content='2026-02-03'>"
            + "<meta itemprop='image' content='/cover.jpg'>"
            + "<div class='post-content' itemprop='articleBody'>"
            + "<div class='dplayer' data-config='{&quot;video&quot;:"
            + "{&quot;url&quot;:&quot;/media/main.m3u8&quot;,"
            + "&quot;type&quot;:&quot;hls&quot;}}'></div>"
            + "</div></article></div></body></html>";

    @Test
    void extractsMetadataAndHlsStream() throws Exception {
        final FakePreferences preferences = preferences("https://site.test");
        final FakeDownloader downloader = new FakeDownloader()
                .respond("https://site.test/archives/7/", ARTICLE);
        NewPipe.init(downloader, Localization.DEFAULT);
        final AacgService service = new AacgService(2, new FakePluginContext(preferences));
        final LinkHandler linkHandler = service.getStreamLHFactory().fromId("7");
        final AacgStreamExtractor extractor = new AacgStreamExtractor(service, linkHandler,
                new AacgSettings(preferences), new AacgSession());

        extractor.fetchPage();

        assertEquals("7", extractor.getId());
        assertEquals("Article title", extractor.getName());
        assertEquals("https://site.test/cover.jpg#aacg", extractor.getThumbnailUrl());
        assertEquals("Summary text", extractor.getDescription().getContent());
        assertEquals(18, extractor.getAgeLimit());
        assertEquals("2026-02-03", extractor.getTextualUploadDate());
        assertEquals("https://site.test", extractor.getUploaderUrl());
        assertEquals("51吃瓜", extractor.getUploaderName());

        final List<VideoStream> streams = extractor.getVideoStreams();
        assertEquals(1, streams.size());
        final VideoStream stream = streams.get(0);
        assertEquals("h264-0", stream.getId());
        assertEquals("https://site.test/media/main.m3u8", stream.getContent());
        assertEquals(DeliveryMethod.HLS, stream.getDeliveryMethod());
        assertEquals(MediaFormat.MPEG_4, stream.getFormat());
        assertEquals(Boolean.FALSE, stream.getCacheable());
        assertEquals("https://site.test/", stream.getRequestHeaders().get("Referer"));
    }

    @Test
    void detectsProgressiveMp4() throws Exception {
        final String html = ARTICLE.replace("/media/main.m3u8", "/media/main.mp4")
                .replace("&quot;hls&quot;", "&quot;normal&quot;");
        final FakePreferences preferences = preferences("https://site.test");
        final FakeDownloader downloader = new FakeDownloader()
                .respond("https://site.test/archives/7/", html);
        NewPipe.init(downloader, Localization.DEFAULT);
        final AacgService service = new AacgService(2, new FakePluginContext(preferences));
        final AacgStreamExtractor extractor = new AacgStreamExtractor(service,
                service.getStreamLHFactory().fromId("7"), new AacgSettings(preferences),
                new AacgSession());
        extractor.fetchPage();
        assertEquals(DeliveryMethod.PROGRESSIVE_HTTP,
                extractor.getVideoStreams().get(0).getDeliveryMethod());
    }

    private static FakePreferences preferences(final String base) {
        final Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("entry_url", base + "/");
        defaults.put("last_base", base);
        defaults.put("decrypt_images", "true");
        return new FakePreferences(defaults);
    }
}
