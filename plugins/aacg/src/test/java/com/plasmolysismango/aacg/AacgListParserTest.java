package com.plasmolysismango.aacg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AacgListParserTest {
    private static final String FEED = "<html><body><div id='index' role='main'>"
            + "<h1 class='blog-title'>51吃瓜 Headline</h1>"
            + "<article itemtype='https://schema.org/BlogPosting'>"
            + "<meta itemprop='mainEntityOfPage' content='/archives/123/'>"
            + "<h2 itemprop='headline'>First post <div class='wrap'>AD</div></h2>"
            + "<span itemprop='datePublished' content='2026-01-02'></span>"
            + "<script>loadBannerDirect('/covers/first.jpg')</script>"
            + "</article>"
            + "<article itemtype='https://schema.org/BlogPosting'>"
            + "<meta itemprop='mainEntityOfPage' content='https://site.test/archives/456/'>"
            + "<h2 itemprop='headline'><div class='wrap'>Promoted</div></h2>"
            + "</article>"
            + "<span class='page-info'>2 / 5</span>"
            + "<ul class='page-navigator'><li class='active'>2</li>"
            + "<li class='prev'><a href='/page/1/'>prev</a></li>"
            + "<li class='next'><a href='/page/3/'>next</a></li></ul>"
            + "</div></body></html>";

    @Test
    void parsesCardsAndPagination() {
        final AacgListParser.ListPage page = AacgListParser.parse(FEED, "https://site.test/");
        assertEquals("51吃瓜 Headline", page.title);
        assertEquals(1, page.items.size());
        final AacgListParser.Card card = page.items.get(0);
        assertEquals("https://site.test/archives/123/", card.url);
        assertEquals("First post", card.title);
        assertEquals("2026-01-02", card.publishedAt);
        assertEquals("https://site.test/covers/first.jpg", card.coverUrl);
        assertEquals(2, page.pagination.current);
        assertEquals(5, page.pagination.total);
        assertEquals("https://site.test/page/3/", page.pagination.nextUrl);
        assertEquals("https://site.test/page/1/", page.pagination.previousUrl);
    }

    @Test
    void parsesCategoryTitleFromBreadcrumb() {
        final String html = "<html><body><div id='archive' role='main'>"
                + "<div class='nav-breadcrumb-wrap'>"
                + "<a href='/category/popular/'>Popular</a>"
                + "<a href='/'>Home</a><span class='name'>Fallback</span></div>"
                + "<article itemtype='https://schema.org/BlogPosting'>"
                + "<meta itemprop='mainEntityOfPage' content='/archives/1/'>"
                + "<h2 itemprop='headline'>One</h2></article>"
                + "</div></body></html>";
        final AacgListParser.ListPage page = AacgListParser.parse(html, "https://site.test/");
        assertEquals("Popular", page.title);
        assertEquals(1, page.items.size());
        assertNull(page.pagination.nextUrl);
        assertEquals(1, page.pagination.current);
        assertEquals(1, page.pagination.total);
    }

    @Test
    void keepsCoverWithoutScript() {
        final String html = "<html><body><div id='index' role='main'>"
                + "<article itemtype='https://schema.org/BlogPosting'>"
                + "<meta itemprop='mainEntityOfPage' content='/archives/9/'>"
                + "<h2 itemprop='headline'>No cover</h2></article>"
                + "</div></body></html>";
        final AacgListParser.ListPage page = AacgListParser.parse(html, "https://site.test/");
        assertNotNull(page.items.get(0));
        assertTrue(page.items.get(0).coverUrl.isEmpty());
    }
}
