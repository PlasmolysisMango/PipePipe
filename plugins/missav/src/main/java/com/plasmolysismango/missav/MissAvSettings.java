package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Urls;

import org.schabi.newpipe.extractor.plugin.PluginPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

/** Values of the settings declared in plugin.json. */
public final class MissAvSettings {
    public static final String DEFAULT_BASE_URL = "https://missav.ws";
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Mobile Safari/537.36";

    private final PluginPreferences preferences;

    public MissAvSettings(final PluginPreferences preferences) {
        this.preferences = preferences;
    }

    public String baseUrl() {
        String value = preferences.getString("base_url");
        if (value == null || value.trim().isEmpty()) {
            value = DEFAULT_BASE_URL;
        }
        value = value.trim();
        if (!Urls.isAbsolute(value)) {
            value = "https://" + value;
        }
        return Urls.normalizeBase(value);
    }

    public String userAgent() {
        final String value = preferences.getString("user_agent");
        return value == null || value.trim().isEmpty() ? DEFAULT_USER_AGENT : value.trim();
    }

    public String variant() {
        final String value = preferences.getString("variant");
        if ("uncensored".equalsIgnoreCase(value)) {
            return "uncensored";
        }
        if ("normal".equalsIgnoreCase(value)) {
            return "normal";
        }
        return "cnsub";
    }

    public String cookie() {
        final String value = preferences.getString("login_cookies");
        return value == null ? "" : value.trim();
    }

    public Map<String, String> requestHeaders() {
        return requestHeaders(baseUrl());
    }

    /** Headers for a request, with the Referer pointing at the given site base. */
    public Map<String, String> requestHeaders(final String refererBase) {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", userAgent());
        headers.put("Referer", refererBase + "/");
        headers.put("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        final String cookie = cookie();
        if (!cookie.isEmpty()) {
            headers.put("Cookie", cookie);
        }
        return headers;
    }

    /** Thumbnail headers, only for urls this plugin marked with the {@code #missav} fragment. */
    public Map<String, String> imageHeaders(final String imageUrl) {
        if (!Urls.hasFragment(imageUrl, "missav")) {
            return null;
        }
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", userAgent());
        headers.put("Referer", baseUrl() + "/");
        final String cookie = cookie();
        if (!cookie.isEmpty()) {
            headers.put("Cookie", cookie);
        }
        return headers;
    }
}
