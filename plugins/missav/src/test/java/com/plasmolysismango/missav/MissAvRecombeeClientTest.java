package com.plasmolysismango.missav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.plasmolysismango.ppipecore.test.FakeDownloader;
import com.plasmolysismango.ppipecore.test.FakePluginContext;
import com.plasmolysismango.ppipecore.test.FakePreferences;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.localization.Localization;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class MissAvRecombeeClientTest {

    private static final String RESPONSE_JSON = "{\"recomms\":["
            + "{\"id\":\"ssis-001\",\"values\":{\"title\":\"SSIS-001 タイトル\","
            + "\"duration\":600,\"released_at\":1600000000}},"
            + "{\"id\":\"ipx-88\",\"values\":{\"title\":\"IPX-088 title\",\"duration\":0}},"
            + "{\"id\":\"\",\"values\":{\"title\":\"ignored\"}}"
            + "]}";

    @Test
    void signsLikeTheFrontend() {
        assertEquals("d503a4661d83bfea076ddcc1826df2a90fec93db",
                MissAvRecombeeClient.signature(
                        "/missav-default/search/users/x/items/?frontend_timestamp=1"));
    }

    @Test
    void mapsRecommendationsToStreamItems() throws Exception {
        final FakeDownloader downloader = new FakeDownloader();
        NewPipe.init(downloader, Localization.DEFAULT);
        downloader.handler(request -> response(request, RESPONSE_JSON));

        final Map<String, String> defaults = new HashMap<>();
        defaults.put("base_url", "https://missav.ws");
        final FakePreferences preferences = new FakePreferences(defaults);
        final MissAvService service = new MissAvService(1, new FakePluginContext(preferences));
        final SearchQueryHandler query = service.getSearchQHFactory().fromQuery("lafbd-41");
        final MissAvSearchExtractor extractor = new MissAvSearchExtractor(service, query,
                new MissAvSettings(preferences));

        final ListExtractor.InfoItemsPage<InfoItem> page = extractor.getInitialPage();
        assertEquals(2, page.getItems().size());

        final InfoItem first = page.getItems().get(0);
        assertEquals("タイトル", first.getName());
        assertEquals("https://missav.ws/cn/ssis-001", first.getUrl());

        final InfoItem second = page.getItems().get(1);
        assertEquals("https://missav.ws/cn/ipx-88", second.getUrl());

        final Request request = downloader.lastRequest();
        assertEquals("POST", request.httpMethod());
        assertTrue(request.url().startsWith(
                "https://client-rapi-missav.recombee.com/missav-default/search/users/anon_"));
        assertTrue(request.url().contains("frontend_sign="));
        assertEquals(Collections.singletonList("application/json"),
                request.headers().get("Content-Type"));
        final String body = new String(request.dataToSend(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"searchQuery\":\"lafbd-41\""));
        assertTrue(body.contains("\"count\":36"));
    }

    private static Response response(final Request request, final String body) {
        final Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));
        return new Response(200, "OK", headers, body,
                body.getBytes(StandardCharsets.UTF_8), request.url());
    }
}
