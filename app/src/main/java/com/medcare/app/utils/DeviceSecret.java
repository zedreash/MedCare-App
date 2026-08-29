package com.medcare.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class DeviceSecret {
    private static final String ALIAS = "medcare_secret_key";
    private static final String PREFS_NAME = "medcare_secure";
    private static final String KEY_ENC = "secret_enc";

    private DeviceSecret() {}

    public static byte[] getOrCreate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String enc = prefs.getString(KEY_ENC, null);
        if (enc != null) {
            try {
                return decryptWithKeystore(Base64.decode(enc, Base64.NO_WRAP));
            } catch (Exception ignored) {
            }
        }
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        try {
            byte[] wrapped = encryptWithKeystore(secret);
            prefs.edit().putString(KEY_ENC, Base64.encodeToString(wrapped, Base64.NO_WRAP)).commit();
        } catch (Exception ignored) {
        }
        return secret;
    }

    public static byte[] encrypt(Context context, byte[] plain) throws Exception {
        byte[] keyBytes = getOrCreate(context);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(plain);
        ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
        buf.put(iv);
        buf.put(ct);
        return buf.array();
    }

    public static byte[] decrypt(Context context, byte[] data) throws Exception {
        byte[] keyBytes = getOrCreate(context);
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte[] iv = new byte[12];
        buf.get(iv);
        byte[] ct = new byte[buf.remaining()];
        buf.get(ct);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(128, iv));
        return cipher.doFinal(ct);
    }

    private static SecretKey keystoreKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            return (SecretKey) ks.getKey(ALIAS, null);
        }
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return kg.generateKey();
    }

    private static byte[] encryptWithKeystore(byte[] plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey());
        byte[] ct = cipher.doFinal(plain);
        byte[] iv = cipher.getIV();
        ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
        buf.put(iv);
        buf.put(ct);
        return buf.array();
    }

    private static byte[] decryptWithKeystore(byte[] data) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte[] iv = new byte[12];
        buf.get(iv);
        byte[] ct = new byte[buf.remaining()];
        buf.get(ct);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), new GCMParameterSpec(128, iv));
        return cipher.doFinal(ct);
    }
}