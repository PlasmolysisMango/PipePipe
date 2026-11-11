package com.plasmolysismango.missav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.plasmolysismango.ppipecore.test.FakeDownloader;
import com.plasmolysismango.ppipecore.test.FakePreferences;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.AntiBotException;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class MissAvHttpTest {

    private static final String PLAYABLE = "<html><body>"
            + "<script>var x = 'm3u8|aa|bb|cc|com|surrit|https|video';</script>"
            + "</body></html>";
    private static final String UNPLAYABLE = "<html><body>no source</body></html>";

    @Test
    void triesVariantsInOrderAndSkipsUnplayablePages() throws Exception {
        final FakeDownloader downloader = new FakeDownloader();
        downloader.handler(request -> {
            if (request.url().endsWith("/cn/ssis-001-chinese-subtitle")) {
                return html(request, 200, UNPLAYABLE);
            }
            if (request.url().endsWith("/cn/ssis-001")) {
                return html(request, 200, PLAYABLE);
            }
            return null;
        });

        final MissAvHttp.DetailPage page = MissAvHttp.fetchDetailPage(downloader,
                settings("cnsub"), "SSIS-001");

        assertEquals("normal", page.kind);
        assertEquals("https://missav.ws/cn/ssis-001", page.url);
        final List<Request> requests = downloader.requests();
        assertEquals("https://missav.ws/cn/ssis-001-chinese-subtitle", requests.get(0).url());
        assertEquals("https://missav.ws/cn/ssis-001-uncensored-leak", requests.get(1).url());
        assertEquals("https://missav.ws/cn/ssis-001", requests.get(2).url());
        assertEquals(Collections.singletonList("https://missav.ws/"),
                requests.get(2).headers().get("Referer"));
    }

    @Test
    void fallsBackToAnotherMirrorWhenBlocked() throws Exception {
        final FakeDownloader downloader = new FakeDownloader();
        downloader.handler(request -> {
            if (request.url().startsWith("https://missav.ws/")) {
                return html(request, 200,
                        "<html><title>Just a moment...</title></html>");
            }
            if (request.url().startsWith("https://missav123.com/cn/ssis-001-chinese-subtitle")) {
                return html(request, 200, PLAYABLE);
            }
            return html(request, 404, "missing");
        });

        final MissAvHttp.DetailPage page = MissAvHttp.fetchDetailPage(downloader,
                settings("cnsub"), "SSIS-001");

        assertEquals("https://missav.ws/cn/ssis-001-chinese-subtitle", page.url);
        final Request last = downloader.lastRequest();
        assertEquals(Collections.singletonList("https://missav123.com/"),
                last.headers().get("Referer"));
    }

    @Test
    void reportsBlockingWhenEveryCandidateIsBlocked() {
        final FakeDownloader downloader = new FakeDownloader();
        downloader.handler(request -> html(request, 403, "forbidden"));

        assertThrows(AntiBotException.class,
                () -> MissAvHttp.fetchDetailPage(downloader, settings("cnsub"), "SSIS-001"));
    }

    @Test
    void reportsMissingWhenNoCandidateHasASource() {
        final FakeDownloader downloader = new FakeDownloader();
        downloader.handler(request -> html(request, 200, UNPLAYABLE));

        assertThrows(ContentNotAvailableException.class,
                () -> MissAvHttp.fetchDetailPage(downloader, settings("cnsub"), "SSIS-001"));
    }

    @Test
    void detectsCloudflareChallengePages() {
        assertEquals(true, MissAvHttp.isChallengePage(
                "<html><title>Just a moment...</title></html>"));
        assertEquals(false, MissAvHttp.isChallengePage(PLAYABLE));
        assertEquals(false, MissAvHttp.isChallengePage(""));
    }

    private static MissAvSettings settings(final String variant) {
        final Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("base_url", "https://missav.ws");
        defaults.put("variant", variant);
        return new MissAvSettings(new FakePreferences(defaults));
    }

    private static Response html(final Request request, final int status, final String body) {
        final Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Content-Type", Collections.singletonList("text/html; charset=utf-8"));
        return new Response(status, status == 200 ? "OK" : "Error", headers, body,
                body.getBytes(java.nio.charset.StandardCharsets.UTF_8), request.url());
    }
}
