package com.plasmolysismango.missav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class MissAvUrlsTest {

    @Test
    void padsTrailingDigitsToThree() {
        assertEquals("LAFBD-041", MissAvUrls.padCodeDigits("LAFBD-41"));
        assertEquals("SSIS-001", MissAvUrls.padCodeDigits("SSIS-001"));
        assertEquals("SSIS-1234", MissAvUrls.padCodeDigits("SSIS-1234"));
        assertEquals("ABC", MissAvUrls.padCodeDigits("ABC"));
        assertEquals("ABC-", MissAvUrls.padCodeDigits("ABC-"));
    }

    @Test
    void buildsSlug() {
        assertEquals("lafbd-041", MissAvUrls.slug("LAFBD-41"));
        assertEquals("ssis-001", MissAvUrls.slug(" ssis-001 "));
    }

    @Test
    void extractsCodes() {
        assertEquals("SSIS-001", MissAvUrls.extractCode("/cn/ssis-001-chinese-subtitle"));
        assertEquals("LAFBD-041", MissAvUrls.extractCode("lafbd-041"));
        assertEquals("STCVS-007", MissAvUrls.extractCode("STCVS-007 title"));
        assertEquals("", MissAvUrls.extractCode("no code here"));
    }

    @Test
    void reversesUuidSegments() {
        assertEquals("cc-bb-aa",
                MissAvUrls.extractUuid("x m3u8|aa|bb|cc|com|surrit|https|video y"));
        assertNull(MissAvUrls.extractUuid("nothing"));
    }

    @Test
    void filtersNavigationLinks() {
        assertTrue(MissAvUrls.isNavHref("/cn/search/ssis-001"));
        assertTrue(MissAvUrls.isNavHref("/dm278/chinese-subtitle"));
        assertTrue(MissAvUrls.isNavHref("/cn/actresses"));
        assertFalse(MissAvUrls.isNavHref("/cn/ssis-001"));
        assertFalse(MissAvUrls.isNavHref("/cn/ssis-001-chinese-subtitle"));
    }

    @Test
    void ordersCandidatesByPreferredVariant() {
        final List<MissAvUrls.VariantPage> defaultOrder = MissAvUrls.detailCandidates(
                "https://missav.ws", "SSIS-1", "cnsub");
        assertEquals("https://missav.ws/cn/ssis-001-chinese-subtitle",
                defaultOrder.get(0).url);
        assertEquals("https://missav.ws/cn/ssis-001-uncensored-leak",
                defaultOrder.get(1).url);

        final List<MissAvUrls.VariantPage> uncensored = MissAvUrls.detailCandidates(
                "https://missav.ws", "SSIS-1", "uncensored");
        assertEquals("https://missav.ws/cn/ssis-001-uncensored-leak",
                uncensored.get(0).url);
        assertEquals("https://missav.ws/dm13/cn/ssis-001", uncensored.get(3).url);
    }

    @Test
    void buildsNextPageUrls() {
        assertEquals("https://missav.ws/search/abc",
                MissAvUrls.searchUrl("https://missav.ws", "abc", 1));
        assertEquals("https://missav.ws/search/abc?page=2",
                MissAvUrls.searchUrl("https://missav.ws", "abc", 2));
        assertEquals("https://missav.ws/search/abc?page=3",
                MissAvUrls.pageUrl("https://missav.ws/search/abc?page=2", 3));
        assertEquals("https://missav.ws/new?page=2",
                MissAvUrls.pageUrl("https://missav.ws/new", 2));
        assertEquals(2, MissAvUrls.pageOf("https://missav.ws/new?page=2"));
        assertEquals(1, MissAvUrls.pageOf("https://missav.ws/new"));
    }

    @Test
    void replacesHost() {
        assertEquals("https://missav123.com/cn/ssis-001",
                MissAvUrls.replaceHost("https://missav.ws/cn/ssis-001", "missav123.com"));
    }
}
