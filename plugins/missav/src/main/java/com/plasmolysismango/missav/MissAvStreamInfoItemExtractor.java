package com.plasmolysismango.missav;

import com.plasmolysismango.ppipecore.Urls;

import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import javax.annotation.Nullable;

/** One MissAV list card as a stream info item. */
public final class MissAvStreamInfoItemExtractor implements StreamInfoItemExtractor {
    private final MissAvPageParser.Card card;
    private final String baseUrl;

    public MissAvStreamInfoItemExtractor(final MissAvPageParser.Card card,
                                         final String baseUrl) {
        this.card = card;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getName() {
        return card.title.isEmpty() ? card.code : card.title;
    }

    @Override
    public String getUrl() {
        return card.url;
    }

    @Override
    public String getThumbnailUrl() {
        return card.thumbnailUrl.isEmpty()
                ? "" : Urls.withFragment(card.thumbnailUrl, "missav");
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getDuration() {
        return card.durationSeconds;
    }

    @Override
    public long getViewCount() {
        return -1;
    }

    @Override
    public String getUploaderName() {
        return MissAvService.NAME;
    }

    @Override
    public String getUploaderUrl() {
        return baseUrl;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return null;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() {
        return null;
    }
}
