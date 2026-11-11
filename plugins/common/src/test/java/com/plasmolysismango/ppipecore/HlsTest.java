package com.plasmolysismango.ppipecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class HlsTest {

    @Test
    void parsesAndSortsMasterVariants() {
        final String master = "#EXTM3U\n"
                + "#EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1280x720,"
                + "CODECS=\"avc1.4d401f,mp4a.40.2\",FRAME-RATE=30\n"
                + "720p/index.m3u8\n"
                + "#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360\n"
                + "/360p/index.m3u8\n";
        assertTrue(Hls.isMaster(master));
        final List<Hls.Variant> variants = Hls.parseMaster(master,
                "https://cdn.example/uuid/playlist.m3u8");
        assertEquals(2, variants.size());

        final Hls.Variant low = variants.get(0);
        assertEquals(800000, low.bandwidth);
        assertEquals(640, low.width);
        assertEquals(360, low.height);
        assertEquals("https://cdn.example/360p/index.m3u8", low.url);

        final Hls.Variant high = variants.get(1);
        assertEquals(2000000, high.bandwidth);
        assertEquals(1280, high.width);
        assertEquals(720, high.height);
        assertEquals("avc1.4d401f,mp4a.40.2", high.codecs);
        assertEquals("30", high.frameRate);
        assertEquals("https://cdn.example/uuid/720p/index.m3u8", high.url);
    }

    @Test
    void treatsMediaPlaylistAsNotMaster() {
        final String media = "#EXTM3U\n#EXT-X-TARGETDURATION:10\n#EXTINF:10,\nseg-1.ts\n";
        assertFalse(Hls.isMaster(media));
        assertTrue(Hls.parseMaster(media, "https://cdn.example/x.m3u8").isEmpty());
    }
}
