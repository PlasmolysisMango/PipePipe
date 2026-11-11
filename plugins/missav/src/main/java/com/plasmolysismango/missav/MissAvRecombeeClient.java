package com.plasmolysismango.missav;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.plasmolysismango.ppipecore.Text;
import com.plasmolysismango.ppipecore.Urls;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * MissAV search through its Recombee backend, which is not behind the site's Cloudflare
 * challenge. The request path is signed like the site's frontend does (HMAC-SHA1).
 */
public final class MissAvRecombeeClient {
    private static final String HOST = "client-rapi-missav.recombee.com";
    private static final String DATABASE = "missav-default";
    private static final String TOKEN =
            "Ikkg568nlM51RHvldlPvc2GzZPE9R4XGzaH9Qj4zK9npbbbTly1gj9K4mgRn0QlV";
    private static final String HMAC = "HmacSHA1";

    private MissAvRecombeeClient() {
    }

    public static List<StreamInfoItem> search(final Downloader downloader,
                                              final StreamingService service,
                                              final MissAvSettings settings,
                                              final String keyword,
                                              int count)
            throws IOException, ParsingException {
        if (count <= 0 || count > 100) {
            count = 36;
        }
        final long timestamp = System.currentTimeMillis() / 1000L;
        final String path = "/search/users/" + anonymousUserId() + "/items/";
        final String unsigned = "/" + DATABASE + path + "?frontend_timestamp=" + timestamp;
        final String apiUrl = "https://" + HOST + unsigned + "&frontend_sign="
                + hmacSha1Hex(unsigned);
        final String payload = "{\"searchQuery\":" + jsonString(keyword)
                + ",\"count\":" + count
                + ",\"cascadeCreate\":true,\"returnProperties\":true}";

        final Map<String, List<String>> headers = new LinkedHashMap<>();
        final String origin = Urls.origin(settings.baseUrl());
        put(headers, "Accept", "application/json");
        put(headers, "Content-Type", "application/json");
        put(headers, "Origin", origin);
        put(headers, "Referer", origin + "/");
        final Response recombeeResponse = downloader.post(apiUrl, headers,
                payload.getBytes(StandardCharsets.UTF_8));
        if (recombeeResponse.responseCode() < 200 || recombeeResponse.responseCode() >= 300) {
            throw new IOException("MissAV Recombee returned HTTP "
                    + recombeeResponse.responseCode());
        }
        return parse(service, recombeeResponse.responseBody(), settings.baseUrl());
    }

    private static List<StreamInfoItem> parse(final StreamingService service,
                                              final String body,
                                              final String baseUrl) throws ParsingException {
        final JsonObject root;
        try {
            root = JsonParser.object().from(body);
        } catch (final JsonParserException e) {
            throw new ParsingException("MissAV Recombee: invalid json", e);
        }
        final List<StreamInfoItem> out = new ArrayList<>();
        final JsonArray recomms = root.getArray("recomms");
        if (recomms == null) {
            return out;
        }
        final String origin = Urls.origin(baseUrl);
        for (int i = 0; i < recomms.size(); i++) {
            final JsonObject recommendation = recomms.getObject(i);
            if (recommendation == null) {
                continue;
            }
            final String id = recommendation.getString("id", "").trim();
            if (id.isEmpty()) {
                continue;
            }
            final JsonObject values = recommendation.getObject("values");
            String code = MissAvUrls.extractCode(id.replace('_', '-'));
            final String title = firstNonEmpty(
                    string(values, "title"),
                    string(values, "title_en"),
                    string(values, "title_zh"));
            if (code.isEmpty()) {
                code = MissAvUrls.extractCode(title);
            }
            if (code.isEmpty()) {
                continue;
            }
            final String name = title.isEmpty() ? code : Text.stripCodeFromTitle(title, code);
            final StreamInfoItem item = new StreamInfoItem(service.getServiceId(),
                    origin + "/cn/" + id, name, StreamType.VIDEO_STREAM);
            item.setUploaderName(MissAvService.NAME);
            item.setUploaderUrl(service.getBaseUrl());
            item.setViewCount(-1);
            final double duration = values == null ? 0 : values.getDouble("duration", 0);
            item.setDuration(duration > 0 ? (long) duration : -1);
            final double releasedAt = values == null ? 0 : values.getDouble("released_at", 0);
            if (releasedAt > 0) {
                item.setTextualUploadDate(formatDate((long) releasedAt));
            }
            out.add(item);
        }
        return out;
    }

    private static String string(@Nullable final JsonObject object, final String key) {
        if (object == null) {
            return "";
        }
        final String value = object.getString(key, "");
        return value == null ? "" : value.trim();
    }

    private static String firstNonEmpty(final String... values) {
        for (final String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String formatDate(final long epochSeconds) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(epochSeconds * 1000L));
    }

    private static String anonymousUserId() {
        final byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        final StringBuilder out = new StringBuilder("anon_");
        for (final byte b : bytes) {
            out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        }
        return out.toString();
    }

    static String signature(final String unsigned) {
        return hmacSha1Hex(unsigned);
    }

    private static String hmacSha1Hex(final String value) {
        try {
            final Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(TOKEN.getBytes(StandardCharsets.UTF_8), HMAC));
            final byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            final StringBuilder out = new StringBuilder(digest.length * 2);
            for (final byte b : digest) {
                out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            }
            return out.toString();
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 unavailable", e);
        }
    }

    private static String jsonString(final String value) {
        final StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                    break;
            }
        }
        return out.append('"').toString();
    }

    private static void put(final Map<String, List<String>> headers, final String name,
                            final String value) {
        final List<String> values = new ArrayList<>(1);
        values.add(value);
        headers.put(name, values);
    }
}
