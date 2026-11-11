package com.plasmolysismango.ppipecore;

/** Strict standard Base64 decoder (avoids java.util.Base64 on old Android releases). */
public final class Base64Codec {
    private Base64Codec() {
    }

    private static final int[] DECODE = new int[128];

    static {
        java.util.Arrays.fill(DECODE, -1);
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int i = 0; i < alphabet.length(); i++) {
            DECODE[alphabet.charAt(i)] = i;
        }
    }

    public static byte[] decodeStrict(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("null base64");
        }
        final StringBuilder cleaned = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == ' ' || c == '\t') {
                continue;
            }
            cleaned.append(c);
        }
        final int length = cleaned.length();
        if (length % 4 != 0) {
            throw new IllegalArgumentException("invalid base64 length");
        }
        int padding = 0;
        if (length > 0 && cleaned.charAt(length - 1) == '=') {
            padding++;
            if (cleaned.charAt(length - 2) == '=') {
                padding++;
            }
        }
        final byte[] out = new byte[length / 4 * 3 - padding];
        int outIndex = 0;
        for (int i = 0; i < length; i += 4) {
            int block = 0;
            for (int j = 0; j < 4; j++) {
                final char c = cleaned.charAt(i + j);
                final int digit;
                if (c == '=') {
                    digit = 0;
                } else {
                    if (c >= 128 || DECODE[c] < 0) {
                        throw new IllegalArgumentException("invalid base64 character");
                    }
                    digit = DECODE[c];
                }
                block = (block << 6) | digit;
            }
            if (outIndex < out.length) {
                out[outIndex++] = (byte) (block >> 16);
            }
            if (outIndex < out.length) {
                out[outIndex++] = (byte) (block >> 8);
            }
            if (outIndex < out.length) {
                out[outIndex++] = (byte) block;
            }
        }
        return out;
    }
}
