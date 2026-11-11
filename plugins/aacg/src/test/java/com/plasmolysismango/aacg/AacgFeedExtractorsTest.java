package com.plasmolysismango.aacg;

import com.plasmolysismango.ppipecore.test.FakeDownloader;
import com.plasmolysismango.ppipecore.test.FakePluginContext;
import com.plasmolysismango.ppipecore.test.FakePreferences;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AacgFeedExtractorsTest {
    private static final String FEED = "<html><body><div id='index' role='main'>"
            + "<h1 class='blog-title'>51吃瓜</h1>"
            + "<article itemtype='https://schema.org/BlogPosting'>"
            + "<meta itemprop='mainEntityOfPage' content='/archives/123/'>"
            + "<h2 itemprop='headline'>First post</h2>"
            + "<script>loadBannerDirect('/covers/first.jpg')</script></article>"
            + "<span class='page-info'>1 / 3</span>"
            + "<ul class='page-navigator'><li class='active'>1</li>"
            + "<li class='next'><a href='/page/2/'>next</a></li></ul>"
            + "</div></body></html>";

    @Test
    void searchCommitsItemsAndNextPage() throws Exception {
        final FakePreferences preferences = preferences();
        final FakeDownloader downloader = new FakeDownloader()
                .respond("https://site.test/search/test/", FEED);
        NewPipe.init(downloader, Localization.DEFAULT);
        final AacgService service = new AacgService(3, new FakePluginContext(preferences));
        final AacgSearchExtractor extractor = new AacgSearchExtractor(service,
                service.getSearchQHFactory().fromQuery("test"), new AacgSettings(preferences),
                new AacgSession());
        extractor.fetchPage();

        final List<InfoItem> items = extractor.getInitialPage().getItems();
        assertEquals(1, items.size());
        final StreamInfoItem item = (StreamInfoItem) items.get(0);
        assertEquals("First post", item.getName());
        assertEquals("https://site.test/archives/123/", item.getUrl());
        assertEquals("https://site.test/covers/first.jpg#aacg", item.getThumbnailUrl());
        assertNotNull(extractor.getInitialPage().getNextPage());
    }

    @Test
    void kioskGeneratesNextPage() throws Exception {
        final FakePreferences preferences = preferences();
        final FakeDownloader downloader = new FakeDownloader()
                .respond("https://site.test/", FEED);
        NewPipe.init(downloader, Localization.DEFAULT);
        final AacgService service = new AacgService(3, new FakePluginContext(preferences));
        final AacgSettings settings = new AacgSettings(preferences);
        final AacgKioskLinkHandlerFactory factory = new AacgKioskLinkHandlerFactory(settings);
        final AacgHomeKioskExtractor extractor = new AacgHomeKioskExtractor(service,
                factory.fromId(AacgKioskLinkHandlerFactory.KIOSK_ID),
                AacgKioskLinkHandlerFactory.KIOSK_ID, settings, new AacgSession());
        extractor.fetchPage();

        final InfoItemsPage<StreamInfoItem> items = extractor.getInitialPage();
        assertEquals(1, items.getItems().size());
        assertEquals("https://site.test/page/2/", items.getNextPage().getUrl());
    }

    private static FakePreferences preferences() {
        final Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("entry_url", "https://site.test/");
        defaults.put("last_base", "https://site.test");
        defaults.put("decrypt_images", "true");
        return new FakePreferences(defaults);
    }
}
