package com.plasmolysismango.ppipecore;

/** Detects common raster image formats from their magic bytes. */
public final class ImageMagic {
    private ImageMagic() {
    }

    /** @return the media type, or null when the payload is not a recognised image */
    public static String detect(final byte[] data) {
        if (data == null || data.length < 4) {
            return null;
        }
        if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8
                && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data.length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N'
                && data[3] == 'G' && data[4] == '\r' && data[5] == '\n'
                && data[6] == 0x1A && data[7] == '\n') {
            return "image/png";
        }
        if (data.length >= 6 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F'
                && data[3] == '8' && (data[4] == '7' || data[4] == '9') && data[5] == 'a') {
            return "image/gif";
        }
        if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F'
                && data[3] == 'F' && data[8] == 'W' && data[9] == 'E'
                && data[10] == 'B' && data[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
