package com.plasmolysismango.aacg;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AacgForwardingDetectorTest {
    @Test
    void findsDirectForwardingAnchor() {
        final String html = "<script>document.getElementById('go-home').click();</script>"
                + "<a id='go-home' href='/landing'>go</a>";
        final String url = AacgForwardingDetector.forwardingUrl(
                Jsoup.parse(html, "https://entry.test/page"), "https://entry.test/page");
        assertEquals("https://entry.test/landing", url);
    }

    @Test
    void findsWrappedForwardingAnchor() {
        final String html = "<script>(function(h){window.location.replace(h.href);})"
                + "(document.getElementById('next'));</script>"
                + "<a id='next' href='https://mirror.test/'>go</a>";
        final String url = AacgForwardingDetector.forwardingUrl(
                Jsoup.parse(html, "https://entry.test/"), "https://entry.test/");
        assertEquals("https://mirror.test/", url);
    }

    @Test
    void rejectsAmbiguousAnchors() {
        final String html = "<script>document.getElementById('a').click();</script>"
                + "<a id='a' href='/one'>1</a><a id='a' href='/two'>2</a>";
        assertThrows(IllegalArgumentException.class, () -> AacgForwardingDetector.forwardingUrl(
                Jsoup.parse(html, "https://entry.test/"), "https://entry.test/"));
    }
}
