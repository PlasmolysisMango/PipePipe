package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.localization.DateWrapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import javax.annotation.Nullable;

/** Parses the ISO-8601 dates published in article and card metadata. */
final class AacgDates {
    private AacgDates() {
    }

    @Nullable
    static DateWrapper wrap(@Nullable final String value) {
        final OffsetDateTime parsed = parse(value);
        return parsed == null ? null : new DateWrapper(parsed);
    }

    @Nullable
    static OffsetDateTime parse(@Nullable final String value) {
        if (value == null) {
            return null;
        }
        final String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (final DateTimeParseException ignored) {
            // fall through to the less specific formats
        }
        try {
            return LocalDateTime.parse(text).atOffset(java.time.ZoneOffset.UTC);
        } catch (final DateTimeParseException ignored) {
            // fall through to a date-only value
        }
        try {
            return LocalDate.parse(text).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        } catch (final DateTimeParseException ignored) {
            return null;
        }
    }
}
