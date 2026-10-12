package com.pdv.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash de senha com PBKDF2-HMAC-SHA256 + salt aleatório (somente JDK). */
public final class Seguranca {
    private Seguranca() { }

    public static String hash(String senha) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        Base64.Encoder b64 = Base64.getEncoder();
        return b64.encodeToString(salt) + ":" + b64.encodeToString(pbkdf2(senha, salt));
    }

    public static boolean confere(String senha, String armazenado) {
        String[] p = armazenado.split(":");
        byte[] salt = Base64.getDecoder().decode(p[0]);
        byte[] esperado = Base64.getDecoder().decode(p[1]);
        return MessageDigest.isEqual(esperado, pbkdf2(senha, salt));
    }

    private static byte[] pbkdf2(String senha, byte[] salt) {
        try {
            var spec = new PBEKeySpec(senha.toCharArray(), salt, 65_000, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
