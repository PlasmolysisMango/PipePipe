package com.plasmolysismango.aacg;

import com.plasmolysismango.ppipecore.test.FakePluginContext;
import com.plasmolysismango.ppipecore.test.FakePreferences;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AacgImageCipherTest {
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF,
            (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01, 0x02, 0x03,
            0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x13, 0x14, 0x15};

    @Test
    void passesPlainImagesThrough() throws Exception {
        assertArrayEquals(JPEG, AacgImageCipher.decode(JPEG));
    }

    @Test
    void decryptsSiteCipher() throws Exception {
        final byte[] ciphertext = encrypt(JPEG);
        assertArrayEquals(JPEG, AacgImageCipher.decode(ciphertext));
    }

    @Test
    void rejectsUnknownPayload() {
        assertThrows(GeneralSecurityException.class,
                () -> AacgImageCipher.decode(new byte[]{1, 2, 3, 4}));
    }

    @Test
    void serviceHandlesMarkedImagesOnly() throws Exception {
        final Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("decrypt_images", "true");
        final FakePreferences preferences = new FakePreferences(defaults);
        final AacgService service = new AacgService(0, new FakePluginContext(preferences));
        assertTrue(service.handlesImage("https://site.test/cover.bin#aacg"));
        assertFalse(service.handlesImage("https://site.test/cover.bin"));
        preferences.putBoolean("decrypt_images", false);
        assertFalse(service.handlesImage("https://site.test/cover.bin#aacg"));
    }

    private static byte[] encrypt(final byte[] plain) throws Exception {
        final int padding = 16 - plain.length % 16;
        final byte[] padded = Arrays.copyOf(plain, plain.length + padding);
        Arrays.fill(padded, plain.length, padded.length, (byte) padding);
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec("f5d965df75336270".getBytes(StandardCharsets.US_ASCII),
                        "AES"),
                new IvParameterSpec("97b60394abc2fbe1".getBytes(StandardCharsets.US_ASCII)));
        return cipher.doFinal(padded);
    }
}
