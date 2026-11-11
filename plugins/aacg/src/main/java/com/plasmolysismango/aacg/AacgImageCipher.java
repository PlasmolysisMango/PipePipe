package com.plasmolysismango.aacg;

import com.plasmolysismango.ppipecore.Aes;
import com.plasmolysismango.ppipecore.ImageMagic;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/** Decrypts the site's AES-128-CBC cover images. */
final class AacgImageCipher {
    private static final String KEY = "f5d965df75336270";
    private static final String IV = "97b60394abc2fbe1";

    private AacgImageCipher() {
    }

    /** Returns the payload unchanged when it already carries an image magic. */
    static byte[] decode(final byte[] data) throws GeneralSecurityException {
        if (ImageMagic.detect(data) != null) {
            return data;
        }
        final byte[] plain = Aes.cbcDecryptPkcs7(
                KEY.getBytes(StandardCharsets.US_ASCII),
                IV.getBytes(StandardCharsets.US_ASCII), data);
        if (ImageMagic.detect(plain) == null) {
            throw new GeneralSecurityException("Unrecognized image payload");
        }
        return plain;
    }
}
