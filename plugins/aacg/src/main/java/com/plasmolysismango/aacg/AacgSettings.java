package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.plugin.PluginPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

/** Values of the settings declared in plugin.json. */
public final class AacgSettings {
    public static final String DEFAULT_ENTRY_URL = "https://aacg13.com/";
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Mobile Safari/537.36";

    private static final String LAST_BASE_KEY = "last_base";
    private static final String BASE_URL_KEY = "base_url";

    private final PluginPreferences preferences;

    public AacgSettings(final PluginPreferences preferences) {
        this.preferences = preferences;
    }

    public String entryUrl() {
        final String value = preferences.getString("entry_url");
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_ENTRY_URL;
        }
        try {
            return AacgUrls.normalize(value);
        } catch (final IllegalArgumentException e) {
            return DEFAULT_ENTRY_URL;
        }
    }

    /** The discovered mirror from earlier sessions, or the configured base as a guess. */
    public String baseUrl() {
        final String last = lastBase();
        return last.isEmpty() ? urlBase() : last;
    }

    /** The base the user configured, ignoring discovered mirrors. */
    public String urlBase() {
        final String value = preferences.getString(BASE_URL_KEY);
        if (value == null || value.trim().isEmpty()) {
            return entryUrl();
        }
        try {
            return AacgUrls.normalize(value);
        } catch (final IllegalArgumentException e) {
            return entryUrl();
        }
    }

    public String lastBase() {
        final String value = preferences.getString(LAST_BASE_KEY);
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            return AacgUrls.trimTrailingSlashes(AacgUrls.normalize(value));
        } catch (final IllegalArgumentException e) {
            return "";
        }
    }

    public void setLastBase(final String url) {
        try {
            preferences.putString(LAST_BASE_KEY, AacgUrls.trimTrailingSlashes(
                    AacgUrls.normalize(url)));
        } catch (final IllegalArgumentException e) {
            // A bad route from the site must not corrupt the stored mirror.
        }
    }

    public boolean decryptImages() {
        return preferences.getBoolean("decrypt_images");
    }

    public String userAgent() {
        return DEFAULT_USER_AGENT;
    }

    public Map<String, String> pageHeaders(final String referer) {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", userAgent());
        headers.put("Accept", "text/html,application/xhtml+xml");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        if (referer != null && !referer.isEmpty()) {
            headers.put("Referer", referer);
        }
        return headers;
    }

    public Map<String, String> imageHeaders() {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", userAgent());
        headers.put("Accept", "image/*,*/*");
        return headers;
    }
}
