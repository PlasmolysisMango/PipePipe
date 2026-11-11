package com.plasmolysismango.aacg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AacgUrlsTest {
    @Test
    void normalizesHostAndStripsFragment() {
        assertEquals("https://example.com/a",
                AacgUrls.normalize("EXAMPLE.com/a#frag"));
        assertEquals("https://example.com:8443/a",
                AacgUrls.normalize("https://EXAMPLE.com:8443/a"));
    }

    @Test
    void rejectsUnsafeTargets() {
        assertThrows(IllegalArgumentException.class,
                () -> AacgUrls.normalize("http://127.0.0.1/"));
        assertThrows(IllegalArgumentException.class,
                () -> AacgUrls.normalize("http://localhost/"));
        assertThrows(IllegalArgumentException.class,
                () -> AacgUrls.normalize("http://192.168.1.2/"));
        assertThrows(IllegalArgumentException.class,
                () -> AacgUrls.normalize("https://user:pass@example.com/"));
        assertThrows(IllegalArgumentException.class,
                () -> AacgUrls.normalize("ftp://example.com/"));
        assertThrows(IllegalArgumentException.class,
                () -> AacgUrls.normalize("https://example.com\\path"));
    }

    @Test
    void resolvesAndComparesOrigins() {
        assertEquals("https://site.test/archives/1/",
                AacgUrls.resolve("https://site.test/archives/", "1/"));
        assertTrue(AacgUrls.sameOrigin("https://SITE.test/a", "https://site.test:443/b"));
        assertFalse(AacgUrls.sameOrigin("https://site.test/a", "http://site.test/a"));
    }

    @Test
    void marksAndStripsFragments() {
        assertTrue(AacgUrls.hasFragment("https://x.test/i#aacg", "aacg"));
        assertTrue(AacgUrls.hasFragment("https://x.test/dir/#aacg", "aacg"));
        assertFalse(AacgUrls.hasFragment("https://x.test/i#aacg#aacg", "aacg"));
        assertEquals("https://x.test/i", AacgUrls.stripFragment("https://x.test/i#aacg"));
        assertEquals("https://x.test/i#aacg",
                AacgUrls.withFragment("https://x.test/i", "aacg"));
    }

    @Test
    void readsPathSegments() {
        assertEquals(2, AacgUrls.pathSegments("https://x.test/archives/12/").size());
        assertEquals("12", AacgUrls.pathSegments("https://x.test/archives/12/").get(1));
    }
}
