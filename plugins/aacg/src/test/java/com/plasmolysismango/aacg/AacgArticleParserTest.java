package com.plasmolysismango.aacg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AacgArticleParserTest {
    private static final String PLAYER_H264 = "{&quot;video&quot;:{&quot;url&quot;:"
            + "&quot;/media/main.m3u8&quot;,&quot;type&quot;:&quot;hls&quot;},"
            + "&quot;video_h265&quot;:[{&quot;url&quot;:&quot;/media/hevc.mp4&quot;,"
            + "&quot;type&quot;:&quot;normal&quot;}]}";
    private static final String PLAYER_OBJECT = "{&quot;video&quot;:{&quot;url&quot;:"
            + "&quot;/media/main.m3u8&quot;,&quot;type&quot;:&quot;hls&quot;},"
            + "&quot;video_h265&quot;:{&quot;url&quot;:&quot;/media/hevc.mp4&quot;,"
            + "&quot;type&quot;:&quot;normal&quot;}}";

    private static String article(final String players) {
        return "<html><head><meta name='description' content='Summary text'></head>"
                + "<body><div id='post' role='main'>"
                + "<article itemtype='https://schema.org/BlogPosting'>"
                + "<h1 itemprop='headline'>Article title</h1>"
                + "<meta itemprop='datePublished' content='2026-02-03'>"
                + "<meta itemprop='image' content='/covers/cover.jpg'>"
                + "<div class='post-content' itemprop='articleBody'>" + players + "</div>"
                + "</article></div></body></html>";
    }

    @Test
    void parsesMetadataAndMergesH265IntoSources() {
        final AacgArticleParser.Article parsed = AacgArticleParser.parse(
                article("<div class='dplayer' data-config='" + PLAYER_H264 + "'></div>"),
                "https://site.test/archives/7/");
        assertEquals("Article title", parsed.title);
        assertEquals("Summary text", parsed.summary);
        assertEquals("2026-02-03", parsed.publishedAt);
        assertEquals("https://site.test/covers/cover.jpg", parsed.coverUrl);
        assertEquals(1, parsed.videos.size());
        final AacgArticleParser.VideoLink video = parsed.videos.get(0);
        assertEquals("https://site.test/media/main.m3u8", video.url);
        assertEquals("hls", video.type);
        assertEquals(2, video.sources.size());
        assertEquals("https://site.test/media/hevc.mp4", video.sources.get(1));
    }

    @Test
    void acceptsObjectAndEmptyVideoH265() {
        final AacgArticleParser.Article parsed = AacgArticleParser.parse(
                article("<div class='dplayer' data-config='" + PLAYER_OBJECT + "'></div>"
                        + "<div class='dplayer' data-config='{&quot;video&quot;:"
                        + "{&quot;url&quot;:&quot;/media/other.mp4&quot;,"
                        + "&quot;type&quot;:&quot;normal&quot;},"
                        + "&quot;video_h265&quot;:null}'></div>"),
                "https://site.test/archives/7/");
        assertEquals(2, parsed.videos.size());
        assertEquals("https://site.test/media/main.m3u8", parsed.videos.get(0).url);
        assertEquals("normal", parsed.videos.get(1).type);
        assertEquals(1, parsed.videos.get(1).sources.size());
    }

    @Test
    void deduplicatesSamePrimaryAcrossPlayers() {
        final AacgArticleParser.Article parsed = AacgArticleParser.parse(
                article("<div class='dplayer' data-config='" + PLAYER_H264 + "'></div>"
                        + "<div class='dplayer' data-config='" + PLAYER_OBJECT + "'></div>"),
                "https://site.test/archives/7/");
        assertEquals(1, parsed.videos.size());
    }

    @Test
    void rejectsPlayerWithoutConfig() {
        assertThrows(IllegalArgumentException.class, () -> AacgArticleParser.parse(
                article("<div class='dplayer'></div>"), "https://site.test/archives/7/"));
    }

    @Test
    void keepsH265OnlyPlayer() {
        final AacgArticleParser.Article parsed = AacgArticleParser.parse(
                article("<div class='dplayer' data-config='{&quot;video_h265&quot;:"
                        + "{&quot;url&quot;:&quot;/media/hevc.mp4&quot;,"
                        + "&quot;type&quot;:&quot;normal&quot;}}'></div>"),
                "https://site.test/archives/7/");
        assertEquals(1, parsed.videos.size());
        assertTrue(parsed.videos.get(0).url.endsWith("/media/hevc.mp4"));
    }
}
