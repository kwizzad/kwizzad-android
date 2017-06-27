package com.kwizzad.db;


import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AES {

    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    private static final String CHARSET = "UTF-8";

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final byte[] ivBytes = {7, 11, 88, 9, 126, -5, -8, 9, 66, -77, -120, 87, -10, 51, 91, -33};

    private static SecretKeySpec generateKey(final String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] bytes = password.getBytes("UTF-8");
        digest.update(bytes, 0, bytes.length);
        byte[] key = digest.digest();

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        return secretKeySpec;
    }


    public static String encrypt(final String password, String message)
            throws GeneralSecurityException {

        try {
            final SecretKeySpec key = generateKey(password);

            byte[] cipherText = encrypt(key, ivBytes, message.getBytes(CHARSET));

            String encoded = Base64.encodeToString(cipherText, Base64.NO_WRAP);

            return encoded;
        } catch (UnsupportedEncodingException e) {
            throw new GeneralSecurityException(e);
        }
    }


    private static byte[] encrypt(final SecretKeySpec key, final byte[] iv, final byte[] message)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(AES_MODE, "BC");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] cipherText = cipher.doFinal(message);

        return cipherText;
    }


    public static String decrypt(final String password, String base64EncodedCipherText)
            throws GeneralSecurityException {

        try {
            final SecretKeySpec key = generateKey(password);

            byte[] decodedCipherText = Base64.decode(base64EncodedCipherText, Base64.NO_WRAP);

            byte[] decryptedBytes = decrypt(key, ivBytes, decodedCipherText);

            String message = new String(decryptedBytes, CHARSET);

            return message;
        } catch (UnsupportedEncodingException e) {
            throw new GeneralSecurityException(e);
        }
    }

    private static byte[] decrypt(final SecretKeySpec key, final byte[] iv, final byte[] decodedCipherText)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(AES_MODE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(decodedCipherText);

        return decryptedBytes;
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private AES() {
    }
}