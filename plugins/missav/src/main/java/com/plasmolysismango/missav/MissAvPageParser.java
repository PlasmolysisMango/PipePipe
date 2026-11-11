package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Text;
import com.plasmolysismango.ppipecore.Urls;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** jsoup based parsing of MissAV list and detail pages. */
public final class MissAvPageParser {
    private MissAvPageParser() {
    }

    public static final class Card {
        public final String code;
        public final String url;
        public final String title;
        public final String thumbnailUrl;
        public final long durationSeconds;

        Card(final String code, final String url, final String title,
             final String thumbnailUrl, final long durationSeconds) {
            this.code = code;
            this.url = url;
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
            this.durationSeconds = durationSeconds;
        }
    }

    public static final class Detail {
        public String title = "";
        public String thumbnailUrl = "";
        public final List<String> tags = new ArrayList<>();
    }

    public static Detail parseDetail(final String html, final String baseUrl) {
        final Document doc = Jsoup.parse(html, baseUrl);
        final Detail detail = new Detail();
        Element title = doc.selectFirst("meta[property=\"og:title\"]");
        if (title == null) {
            title = doc.selectFirst("meta[name=\"title\"]");
        }
        if (title != null) {
            detail.title = Text.clean(title.attr("content"));
            final String code = MissAvUrls.extractCode(detail.title);
            if (!code.isEmpty()) {
                detail.title = Text.stripCodeFromTitle(detail.title, code);
            }
        }
        final Element image = doc.selectFirst("meta[property=\"og:image\"]");
        if (image != null) {
            final String src = image.attr("content").trim();
            if (!src.isEmpty()) {
                detail.thumbnailUrl = Urls.resolve(baseUrl, src);
            }
        }
        final Set<String> seen = new LinkedHashSet<>();
        for (final Element link : doc.select(
                "a[href*=actress], a[href*=actor], a[href*=genre], a[href*=tag], .tag")) {
            final String tag = Text.clean(link.text());
            if (!tag.isEmpty()) {
                seen.add(tag);
            }
        }
        detail.tags.addAll(seen);
        return detail;
    }

    public static List<Card> parseCards(final String html, final String baseUrl) {
        final Document doc = Jsoup.parse(html, baseUrl);
        final List<Card> cards = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final Element link : doc.select("a[href]")) {
            final String href = link.attr("href");
            if (href.isEmpty() || MissAvUrls.isNavHref(href)) {
                continue;
            }
            final String code = MissAvUrls.extractCode(href);
            if (code.isEmpty() || !seen.add(code)) {
                continue;
            }
            Element container = link.closest("div.group");
            if (container == null) {
                container = link;
            }
            String rawTitle = firstAttribute(container, "img", "title");
            if (rawTitle.isEmpty()) {
                rawTitle = firstAttribute(container, "img", "alt");
            }
            if (rawTitle.isEmpty()) {
                rawTitle = link.text();
            }
            String cardTitle = Text.clean(rawTitle);
            cardTitle = Text.clean(Text.stripCodeFromTitle(cardTitle, code));
            String thumbnail = "";
            final Element image = container.selectFirst("img");
            if (image != null) {
                final String src = pickImageSource(image);
                if (!src.isEmpty()) {
                    thumbnail = Urls.resolve(baseUrl, src);
                }
            }
            final String url = Urls.resolve(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/",
                    href);
            cards.add(new Card(code, url, cardTitle, thumbnail,
                    parseCardDuration(container)));
        }
        return cards;
    }

    private static long parseCardDuration(final Element container) {
        for (final Element span : container.select("span")) {
            final String text = span.text();
            if (text.contains("分") || text.matches(".*\\d{1,2}:\\d{2}:\\d{2}.*")) {
                final long seconds = Text.parseDurationSeconds(text);
                if (seconds > 0) {
                    return seconds;
                }
            }
        }
        return 0;
    }

    private static String firstAttribute(final Element scope, final String selector,
                                         final String attribute) {
        final Element element = scope.selectFirst(selector);
        return element == null ? "" : element.attr(attribute).trim();
    }

    private static String pickImageSource(final Element image) {
        for (final String attribute : new String[]{"data-src", "data-original", "data-lazy",
                "src"}) {
            final String value = image.attr(attribute).trim();
            if (!value.isEmpty() && !value.startsWith("data:")) {
                return value;
            }
        }
        return "";
    }
}
