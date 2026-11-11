package com.plasmolysismango.aacg;

import com.plasmolysismango.ppipecore.test.FakeDownloader;
import com.plasmolysismango.ppipecore.test.FakePreferences;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AacgSessionTest {
    private static final String CONFIG_DATA =
            "AAECAwQFBgcICQoLDA0OD0H1fNDrxsZ2eaVQNyez7HJDxAnLDQn1ieXDaFQ0MMUW/Nz3DyK6ymJBi5IwSApQk5XZ1fRfFQj0l2w6tolT3KsFy+Hf/GAO0ClqbCL48F4S2FfzBjWqi3kaaPuv2nb3NhlYRPsQ8/+TSzAo5hQUuIqFyZRxV+uR+FgPN+N71zF/oSgOMFsWNFTHMwaQSxv/RFgOaOywBADCnmzTLJofLglzItXZzgj5kDLg0x8uit8hrYJan9bwYTVE8Fe9+Y2GWCfwrdRI1mgkim2a4Dnzu734Cr+1Y3vXj5Tz3SHytSmp43vkaw/r6wSl4OF/SOigeVE8W8KSPujSUIsUQEZGWgfZ1cCrllHZMUhrjlie7rdj1P8AyjDMDbnXftuSxDXcS5kQvJBpnyg43FpAsrWH9Ec=";
    private static final String CONFIG_KEY = "synthetic-key-雪";

    @Test
    void followsForwardingThenReadsRoutes() throws Exception {
        final AacgSettings settings = new AacgSettings(new FakePreferences());
        final FakeDownloader downloader = new FakeDownloader()
                .respond("https://aacg13.com/",
                        "<html><body><script>document.getElementById('go').click();"
                                + "</script><a id='go' href='/landing'>go</a></body></html>")
                .respond("https://aacg13.com/landing",
                        "<html><body><script>window.appConfig = {data:'" + CONFIG_DATA
                                + "', key:'" + CONFIG_KEY + "'};</script></body></html>");
        final AacgSession.Site site = new AacgSession().discover(downloader, settings);
        assertEquals("https://example.invalid/primary", site.baseUrl);
        assertEquals(2, site.targets.size());
        assertEquals("primary", site.targets.get(0).kind);
        assertEquals("https://example.invalid/primary", settings.lastBase());
    }

    @Test
    void fallsBackToStoredMirror() throws Exception {
        final FakePreferences preferences = new FakePreferences();
        preferences.putString("last_base", "https://mirror.test/");
        final AacgSettings settings = new AacgSettings(preferences);
        final FakeDownloader downloader = new FakeDownloader().handler(request -> {
            throw new IOException("offline");
        });
        final AacgSession.Site site = new AacgSession().site(downloader, settings);
        assertEquals("https://mirror.test", site.baseUrl);
    }

    @Test
    void rejectsConfigWithoutRoutes() {
        final FakeDownloader downloader = new FakeDownloader().respond("https://aacg13.com/",
                "<script>window.appConfig = {data:'AAAA', key:'k'};</script>");
        final AacgSettings settings = new AacgSettings(new FakePreferences());
        assertThrows(org.schabi.newpipe.extractor.exceptions.ExtractionException.class,
                () -> new AacgSession().discover(downloader, settings));
    }
}
