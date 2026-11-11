package com.plasmolysismango.aacg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AacgDatesTest {

    @Test
    void parsesOffsetDateTime() {
        assertEquals("2024-01-02T03:04:05+08:00",
                AacgDates.parse("2024-01-02T03:04:05+08:00").toString());
    }

    @Test
    void parsesLocalDateTimeAsUtc() {
        assertEquals("2024-01-02T03:04:05Z",
                AacgDates.parse("2024-01-02T03:04:05").toInstant().toString());
    }

    @Test
    void parsesDateOnly() {
        assertEquals("2024-01-02T00:00:00Z",
                AacgDates.parse("2024-01-02").toInstant().toString());
    }

    @Test
    void rejectsMissingAndGarbage() {
        assertNull(AacgDates.parse(null));
        assertNull(AacgDates.parse(""));
        assertNull(AacgDates.parse("yesterday"));
        assertNull(AacgDates.wrap("  "));
    }
}
