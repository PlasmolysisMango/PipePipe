package com.plasmolysismango.aacg;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.plasmolysismango.ppipecore.Text;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/** Parses one article page: metadata, cover and DPlayer sources. */
public final class AacgArticleParser {
    public static final class VideoLink {
        public final String url;
        public final String type;
        public final List<String> sources;

        VideoLink(final String url, final String type, final List<String> sources) {
            this.url = url;
            this.type = type;
            this.sources = sources;
        }
    }

    public static final class Article {
        public final String url;
        public final String title;
        public final String summary;
        public final String publishedAt;
        public final String coverUrl;
        public final List<VideoLink> videos;

        Article(final String url, final String title, final String summary,
                final String publishedAt, final String coverUrl,
                final List<VideoLink> videos) {
            this.url = url;
            this.title = title;
            this.summary = summary;
            this.publishedAt = publishedAt;
            this.coverUrl = coverUrl;
            this.videos = videos;
        }
    }

    private AacgArticleParser() {
    }

    public static Article parse(final String html, final String baseUrl) {
        final Document document = Jsoup.parse(html, baseUrl);
        final Element post = one(document.select("div#post[role=main]"), "article page");
        final Element article = one(post.select("article[itemtype$='/BlogPosting']"),
                "article");
        final Element headline = one(article.select("h1[itemprop~=headline]"),
                "article headline");
        final String title = Text.collapseWhitespace(headline.text());
        if (title.isEmpty()) {
            throw new IllegalArgumentException("51吃瓜 article: empty article title");
        }
        final String summary = emptyIfNull(meta(document.select("meta[name=description]"),
                "description"));
        final String publishedAt = emptyIfNull(
                meta(article.select("meta[itemprop=datePublished]"), "article date"));
        final String cover = emptyIfNull(
                meta(article.select("meta[itemprop=image]"), "article cover"));
        final String coverUrl;
        try {
            coverUrl = cover.isEmpty() ? "" : AacgUrls.resolve(baseUrl, cover);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("51吃瓜 article: invalid cover URL", e);
        }
        one(article.select("div.post-content[itemprop=articleBody]"), "article body");
        final List<VideoLink> videos = articleVideos(article, baseUrl);
        return new Article(baseUrl, title, summary, publishedAt, coverUrl, videos);
    }

    private static String emptyIfNull(@Nullable final String value) {
        return value == null ? "" : value;
    }

    static String meta(final Elements elements, final String label) {
        if (elements.size() > 1) {
            throw new IllegalArgumentException("51吃瓜 article: ambiguous " + label);
        }
        return elements.isEmpty() ? null : elements.first().attr("content").trim();
    }

    private static List<VideoLink> articleVideos(final Element article, final String baseUrl) {
        final List<VideoLink> videos = new ArrayList<>();
        final Map<String, Boolean> seen = new LinkedHashMap<>();
        for (final Element player : article.select("div.dplayer")) {
            final String raw = player.attr("data-config").trim();
            if (raw.isEmpty()) {
                throw new IllegalArgumentException("51吃瓜 article: player without config");
            }
            final JsonObject config;
            try {
                config = JsonParser.object().from(raw);
            } catch (final JsonParserException e) {
                throw new IllegalArgumentException(
                        "51吃瓜 article: invalid player config", e);
            }
            final List<JsonObject> order = new ArrayList<>();
            final Object video = config.get("video");
            if (video instanceof JsonObject) {
                order.add((JsonObject) video);
            }
            final Object h265 = config.get("video_h265");
            if (h265 instanceof JsonObject) {
                order.add((JsonObject) h265);
            } else if (h265 instanceof JsonArray) {
                final JsonArray array = (JsonArray) h265;
                for (int i = 0; i < array.size(); i++) {
                    final Object item = array.get(i);
                    if (item instanceof JsonObject) {
                        order.add((JsonObject) item);
                    }
                }
            }
            final List<String> candidates = new ArrayList<>();
            final Map<String, Boolean> seenHere = new LinkedHashMap<>();
            String primaryType = "";
            for (final JsonObject source : order) {
                final String address;
                try {
                    address = AacgUrls.resolve(baseUrl, stringValue(source, "url"));
                } catch (final IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "51吃瓜 article: invalid video link", e);
                }
                if (seenHere.containsKey(address)) {
                    continue;
                }
                seenHere.put(address, Boolean.TRUE);
                if (candidates.isEmpty()) {
                    primaryType = stringValue(source, "type").trim();
                }
                candidates.add(address);
            }
            if (candidates.isEmpty() || seen.containsKey(candidates.get(0))) {
                continue;
            }
            seen.put(candidates.get(0), Boolean.TRUE);
            videos.add(new VideoLink(candidates.get(0), primaryType, candidates));
        }
        return videos;
    }

    private static String stringValue(final JsonObject object, final String key) {
        final Object value = object.get(key);
        return value instanceof String ? (String) value : "";
    }

    private static Element one(final Elements elements, final String label) {
        if (elements.size() != 1) {
            throw new IllegalArgumentException("51吃瓜 parser: expected one " + label
                    + ", found " + elements.size());
        }
        return elements.first();
    }
}
