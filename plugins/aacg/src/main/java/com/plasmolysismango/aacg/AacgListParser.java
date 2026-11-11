package com.plasmolysismango.aacg;

import com.plasmolysismango.ppipecore.Text;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the shared homepage, category and search archive markup. */
public final class AacgListParser {
    private static final Pattern COVER_PATTERN =
            Pattern.compile("loadBannerDirect\\('([^']+)'");

    public static final class Card {
        public final String url;
        public final String title;
        public final String publishedAt;
        public final String coverUrl;

        Card(final String url, final String title, final String publishedAt,
             final String coverUrl) {
            this.url = url;
            this.title = title;
            this.publishedAt = publishedAt;
            this.coverUrl = coverUrl;
        }
    }

    public static final class Pagination {
        public final int current;
        public final int total;
        public final String nextUrl;
        public final String previousUrl;

        Pagination(final int current, final int total, final String nextUrl,
                   final String previousUrl) {
            this.current = current;
            this.total = total;
            this.nextUrl = nextUrl;
            this.previousUrl = previousUrl;
        }

        static Pagination single() {
            return new Pagination(1, 1, null, null);
        }
    }

    public static final class ListPage {
        public final String title;
        public final List<Card> items;
        public final Pagination pagination;

        ListPage(final String title, final List<Card> items, final Pagination pagination) {
            this.title = title;
            this.items = items;
            this.pagination = pagination;
        }
    }

    private AacgListParser() {
    }

    public static ListPage parse(final String html, final String baseUrl) {
        final Document document = Jsoup.parse(html, baseUrl);
        final Element container = one(document.select(
                "div#index[role=main], div#archive[role=main]"), "feed container");
        final String title = "index".equals(container.id())
                ? feedHeading(document) : listTitle(container, baseUrl);
        final List<Card> items = listCards(container, baseUrl);
        final Pagination pagination = listPagination(container, baseUrl);
        return new ListPage(title, items, pagination);
    }

    public static List<Card> cards(final String html, final String baseUrl) {
        final Document document = Jsoup.parse(html, baseUrl);
        final Element container = one(document.select(
                "div#index[role=main], div#archive[role=main]"), "feed container");
        return listCards(container, baseUrl);
    }

    private static String feedHeading(final Document document) {
        final Elements titles = document.select("h1.blog-title");
        return titles.size() == 1 ? Text.collapseWhitespace(titles.first().text()) : "";
    }

    private static String listTitle(final Element archive, final String baseUrl) {
        final Element wrap = directChild(archive, "div", "nav-breadcrumb-wrap");
        if (wrap == null) {
            throw new IllegalArgumentException("51吃瓜 list: missing breadcrumb");
        }
        String title = "";
        for (final Element link : wrap.select("a[href]")) {
            final String address;
            try {
                address = AacgUrls.resolve(baseUrl, link.attr("href"));
            } catch (final IllegalArgumentException e) {
                continue;
            }
            if (!AacgUrls.path(address).startsWith("/category/")
                    || !AacgUrls.sameOrigin(address, baseUrl)) {
                continue;
            }
            final String text = Text.collapseWhitespace(link.text());
            if (!text.isEmpty()) {
                title = text;
            }
        }
        if (title.isEmpty()) {
            final Element name = directChild(wrap, "span", "name");
            if (name != null) {
                title = Text.collapseWhitespace(name.text());
            }
        }
        if (title.isEmpty()) {
            throw new IllegalArgumentException("51吃瓜 list: empty category title");
        }
        return title;
    }

    private static List<Card> listCards(final Element archive, final String baseUrl) {
        final List<Card> items = new ArrayList<>();
        final Map<String, Boolean> seen = new LinkedHashMap<>();
        for (final Element card : archive.select("article[itemtype$='/BlogPosting']")) {
            final Card item = listCard(card, baseUrl);
            if (item == null) {
                continue;
            }
            if (!seen.containsKey(item.url)) {
                seen.put(item.url, Boolean.TRUE);
                items.add(item);
            }
        }
        return items;
    }

    private static Card listCard(final Element card, final String baseUrl) {
        final Element link = one(card.select("meta[itemprop~=mainEntityOfPage]"), "card link");
        final String url;
        try {
            url = AacgUrls.resolve(baseUrl, link.attr("content"));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("51吃瓜 list: invalid card link", e);
        }
        final Element headline = one(card.select("h2[itemprop=headline]"), "card headline");
        final Element clean = headline.clone();
        clean.select("div.wrap").remove();
        final String title = Text.collapseWhitespace(clean.text());
        if (title.isEmpty()) {
            if (!headline.select("div.wrap").isEmpty()) {
                return null;
            }
            throw new IllegalArgumentException("51吃瓜 list: empty card headline");
        }
        final Elements dates = card.select("span[itemprop=datePublished]");
        if (dates.size() > 1) {
            throw new IllegalArgumentException("51吃瓜 list: ambiguous card date");
        }
        final String publishedAt = dates.isEmpty() ? "" : dates.first().attr("content").trim();
        String coverUrl = "";
        for (final Element script : card.select("script")) {
            final Matcher matcher = COVER_PATTERN.matcher(script.data());
            if (!matcher.find()) {
                continue;
            }
            try {
                coverUrl = AacgUrls.resolve(baseUrl, matcher.group(1));
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("51吃瓜 list: invalid cover URL", e);
            }
            break;
        }
        return new Card(url, title, publishedAt, coverUrl);
    }

    private static Pagination listPagination(final Element archive, final String baseUrl) {
        int current = 1;
        int total = 1;
        final Elements info = archive.select("span.page-info");
        if (info.size() > 1) {
            throw new IllegalArgumentException("51吃瓜 list: ambiguous page info");
        }
        if (info.size() == 1) {
            final int[] parsed = pageInfo(info.first().text());
            current = parsed[0];
            total = parsed[1];
        }
        final Elements navigators = archive.select("ul.page-navigator");
        if (navigators.isEmpty()) {
            return new Pagination(current, total, null, null);
        }
        final Element navigator = one(navigators, "page navigator");
        final Element active = one(navigator.select("li.active"), "active page");
        try {
            current = Integer.parseInt(Text.collapseWhitespace(active.text()));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("51吃瓜 list: invalid current page", e);
        }
        if (current < 1) {
            throw new IllegalArgumentException("51吃瓜 list: invalid current page");
        }
        final String next = link(navigator, "li.next a[href]", baseUrl, "next");
        final String previous = link(navigator, "li.prev a[href]", baseUrl, "previous");
        return new Pagination(current, total, next, previous);
    }

    private static String link(final Element navigator, final String selector,
                               final String baseUrl, final String label) {
        final Elements links = navigator.select(selector);
        if (links.isEmpty()) {
            return null;
        }
        final Element link = one(links, label + " page link");
        try {
            return AacgUrls.resolve(baseUrl, link.attr("href"));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("51吃瓜 list: invalid " + label + " link", e);
        }
    }

    private static int[] pageInfo(final String text) {
        final String collapsed = Text.collapseWhitespace(text);
        final int cut = collapsed.indexOf('/');
        if (cut < 0) {
            throw new IllegalArgumentException("51吃瓜 list: invalid page info");
        }
        try {
            final int current = Integer.parseInt(collapsed.substring(0, cut).trim());
            final int total = Integer.parseInt(collapsed.substring(cut + 1).trim());
            if (current < 1 || total < current) {
                throw new IllegalArgumentException("51吃瓜 list: invalid page info");
            }
            return new int[]{current, total};
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("51吃瓜 list: invalid page info", e);
        }
    }

    private static Element directChild(final Element parent, final String tag,
                                       final String className) {
        for (final Element child : parent.children()) {
            if (child.tagName().equals(tag) && child.hasClass(className)) {
                return child;
            }
        }
        return null;
    }

    private static Element one(final Elements elements, final String label) {
        if (elements.size() != 1) {
            throw new IllegalArgumentException("51吃瓜 parser: expected one " + label
                    + ", found " + elements.size());
        }
        return elements.first();
    }
}
