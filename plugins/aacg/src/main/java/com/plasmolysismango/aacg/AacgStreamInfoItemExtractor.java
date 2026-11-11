package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import javax.annotation.Nullable;

/** One 51吃瓜 archive card as a stream info item. */
public final class AacgStreamInfoItemExtractor implements StreamInfoItemExtractor {
    private final AacgListParser.Card card;
    private final String baseUrl;

    public AacgStreamInfoItemExtractor(final AacgListParser.Card card, final String baseUrl) {
        this.card = card;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getName() {
        return card.title;
    }

    @Override
    public String getUrl() {
        return card.url;
    }

    @Override
    public String getThumbnailUrl() {
        return card.coverUrl.isEmpty() ? "" : AacgUrls.withFragment(card.coverUrl, "aacg");
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getDuration() {
        return -1;
    }

    @Override
    public long getViewCount() {
        return -1;
    }

    @Override
    public String getUploaderName() {
        return AacgService.NAME;
    }

    @Override
    public String getUploaderUrl() {
        return baseUrl;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return card.publishedAt.isEmpty() ? null : card.publishedAt;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() {
        return AacgDates.wrap(card.publishedAt);
    }
}
