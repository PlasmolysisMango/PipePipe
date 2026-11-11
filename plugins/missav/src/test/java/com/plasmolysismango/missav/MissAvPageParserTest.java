package com.plasmolysismango.missav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class MissAvPageParserTest {

    private static final String LIST_HTML = "<html><body>"
            + "<div class='group'>"
            + "  <a href='/cn/ssis-001-chinese-subtitle'>"
            + "    <img data-src='/img/ssis-001.jpg' title='SSIS-001 テストタイトル'>"
            + "    <span>120分</span>"
            + "  </a>"
            + "</div>"
            + "<div class='group'>"
            + "  <a href='/cn/ssis-002'>"
            + "    <img src='https://img.example/ssis-002.jpg' alt='SSIS-002 二本目'>"
            + "    <span>2:13:00</span>"
            + "  </a>"
            + "</div>"
            + "<a href='/cn/ssis-001-uncensored-leak'>duplicate</a>"
            + "<a href='/cn/search/ssis'>search</a>"
            + "<a href='/cn/actresses/abc'>actress</a>"
            + "</body></html>";

    @Test
    void parsesCardsWithoutDuplicates() {
        final List<MissAvPageParser.Card> cards = MissAvPageParser.parseCards(LIST_HTML,
                "https://missav.ws/search/ssis-001");
        assertEquals(2, cards.size());

        final MissAvPageParser.Card first = cards.get(0);
        assertEquals("SSIS-001", first.code);
        assertEquals("https://missav.ws/cn/ssis-001-chinese-subtitle", first.url);
        assertEquals("テストタイトル", first.title);
        assertEquals("https://missav.ws/img/ssis-001.jpg", first.thumbnailUrl);
        assertEquals(120 * 60, first.durationSeconds);

        final MissAvPageParser.Card second = cards.get(1);
        assertEquals("SSIS-002", second.code);
        assertEquals("二本目", second.title);
        assertEquals("https://img.example/ssis-002.jpg", second.thumbnailUrl);
        assertEquals((2 * 60 + 13) * 60, second.durationSeconds);
    }

    private static final String DETAIL_HTML = "<html><head>"
            + "<meta property='og:title' content='SSIS-001 テストタイトル'>"
            + "<meta property='og:image' content='https://img.example/cover.jpg'>"
            + "</head><body>"
            + "<a href='/actresses/foo'>Actress A</a>"
            + "<a href='/genres/bar'>Genre B</a>"
            + "<span class='tag'>Tag C</span>"
            + "<script>var x = 'm3u8|aa|bb|cc|com|surrit|https|video';</script>"
            + "</body></html>";

    @Test
    void parsesDetailMetadata() {
        final MissAvPageParser.Detail detail = MissAvPageParser.parseDetail(DETAIL_HTML,
                "https://missav.ws/cn/ssis-001");
        assertEquals("テストタイトル", detail.title);
        assertEquals("https://img.example/cover.jpg", detail.thumbnailUrl);
        assertTrue(detail.tags.contains("Actress A"));
        assertTrue(detail.tags.contains("Genre B"));
        assertTrue(detail.tags.contains("Tag C"));
    }

    @Test
    void extractsUuidFromPage() {
        assertEquals("cc-bb-aa", MissAvUrls.extractUuid(DETAIL_HTML));
    }
}
