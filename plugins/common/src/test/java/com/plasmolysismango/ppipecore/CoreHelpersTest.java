package com.plasmolysismango.ppipecore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class CoreHelpersTest {

    private static final byte[] KEY = "0123456789abcdef0123456789abcdef"
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] IV = "abcdefghijklmnop".getBytes(StandardCharsets.UTF_8);

    private static byte[] encrypt(final byte[] key, final byte[] iv, final byte[] plain)
            throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));
        return cipher.doFinal(plain);
    }

    @Test
    void decryptsAesCbcPkcs7Utf8() throws Exception {
        final String plain = "hello 世界";
        final byte[] ciphertext = encrypt(KEY, IV, plain.getBytes(StandardCharsets.UTF_8));
        assertEquals(plain, Aes.cbcDecryptUtf8(KEY, IV, ciphertext));
    }

    @Test
    void rejectsInconsistentPadding() throws Exception {
        final byte[] bad = new byte[16];
        bad[15] = 2;
        bad[14] = 3;
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"),
                new IvParameterSpec(IV));
        final byte[] ciphertext = cipher.doFinal(bad);
        assertThrows(GeneralSecurityException.class,
                () -> Aes.cbcDecryptPkcs7(KEY, IV, ciphertext));
    }

    @Test
    void rejectsMalformedUtf8() throws Exception {
        final byte[] bad = new byte[]{(byte) 0xC3, (byte) 0x28, 13, 13,
                13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13};
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"),
                new IvParameterSpec(IV));
        final byte[] ciphertext = cipher.doFinal(bad);
        assertThrows(GeneralSecurityException.class,
                () -> Aes.cbcDecryptUtf8(KEY, IV, ciphertext));
    }

    @Test
    void decodesBase64Strictly() {
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8),
                Base64Codec.decodeStrict("aGVsbG8="));
        assertThrows(IllegalArgumentException.class, () -> Base64Codec.decodeStrict("aGVsbG8"));
    }

    @Test
    void detectsImageMagics() {
        assertEquals("image/jpeg", ImageMagic.detect(new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0}));
        assertEquals("image/png", ImageMagic.detect(new byte[]{
                (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}));
        assertEquals("image/gif", ImageMagic.detect(new byte[]{
                'G', 'I', 'F', '8', '9', 'a'}));
        assertEquals("image/webp", ImageMagic.detect(new byte[]{
                'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}));
        assertNull(ImageMagic.detect(new byte[]{1, 2, 3}));
    }

    @Test
    void cleansAndParsesText() {
        assertEquals("a & b", Text.clean("  a &amp; b  "));
        assertEquals("SSIS-001", Text.stripCodeFromTitle("SSIS-001 Title", "SSIS-001"));
        assertEquals(7200, Text.parseDurationSeconds("120分"));
        assertEquals(7980, Text.parseDurationSeconds("2:13:00"));
        assertEquals(3660, Text.parseDurationSeconds("1小时 1分"));
    }

    @Test
    void resolvesUrls() {
        assertEquals("https://a.example/x/y",
                Urls.resolve("https://a.example/x/", "y"));
        assertEquals("https://a.example/y",
                Urls.resolve("https://a.example/x/", "/y"));
        assertEquals("a.example", Urls.host("https://a.example/x"));
        assertEquals("https://a.example", Urls.origin("https://a.example/x?q=1"));
        assertEquals("https://a.example/b/c", Urls.joinPath("https://a.example", "b", "c"));
        assertEquals("x", Urls.lastPathSegment("https://a.example/b/x/?q=1"));
    }
}
