package com.plasmolysismango.ppipecore;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-CBC helpers with strict PKCS7 and UTF-8 validation. */
public final class Aes {
    private Aes() {
    }

    public static byte[] cbcDecrypt(final byte[] key, final byte[] iv, final byte[] ciphertext)
            throws GeneralSecurityException {
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new GeneralSecurityException("Invalid AES key length " + key.length);
        }
        if (iv.length != 16) {
            throw new GeneralSecurityException("Invalid AES IV length " + iv.length);
        }
        if (ciphertext.length == 0 || ciphertext.length % 16 != 0) {
            throw new GeneralSecurityException("Ciphertext is not block aligned");
        }
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    /** Removes PKCS7 padding, rejecting empty, oversized and inconsistent padding. */
    public static byte[] pkcs7Unpad(final byte[] data) throws GeneralSecurityException {
        if (data.length == 0) {
            throw new GeneralSecurityException("Empty padded data");
        }
        final int padding = data[data.length - 1] & 0xFF;
        if (padding < 1 || padding > 16 || padding > data.length) {
            throw new GeneralSecurityException("Invalid PKCS7 padding");
        }
        for (int i = data.length - padding; i < data.length; i++) {
            if ((data[i] & 0xFF) != padding) {
                throw new GeneralSecurityException("Inconsistent PKCS7 padding");
            }
        }
        return Arrays.copyOf(data, data.length - padding);
    }

    public static byte[] cbcDecryptPkcs7(final byte[] key, final byte[] iv,
                                         final byte[] ciphertext) throws GeneralSecurityException {
        return pkcs7Unpad(cbcDecrypt(key, iv, ciphertext));
    }

    /** Decrypts AES-CBC/PKCS7 and validates the result as UTF-8. */
    public static String cbcDecryptUtf8(final byte[] key, final byte[] iv,
                                        final byte[] ciphertext) throws GeneralSecurityException {
        return decodeUtf8Strict(cbcDecryptPkcs7(key, iv, ciphertext));
    }

    public static String decodeUtf8Strict(final byte[] data) throws GeneralSecurityException {
        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(data)).toString();
        } catch (final CharacterCodingException e) {
            throw new GeneralSecurityException("Invalid UTF-8", e);
        }
    }
}
