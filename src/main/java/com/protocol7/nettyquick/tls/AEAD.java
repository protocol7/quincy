package com.protocol7.nettyquick.tls;

import com.google.common.primitives.Longs;
import com.protocol7.nettyquick.utils.Hex;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class AEAD {

    public static final int OVERHEAD = 16;

    private static byte[] makeNonce(final byte[] iv, final long packetNumber) {
        final byte[] nonce = new byte[iv.length];

        System.arraycopy(Longs.toByteArray(packetNumber), 0, nonce, nonce.length - 8, 8);

        for (int i = 0; i<iv.length; i++) {
            nonce[i] ^= iv[i];
        }
        return nonce;
    }

    private static byte[] prepareKey(final byte[] key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        } else if (key.length != 16) {
            throw new IllegalArgumentException("key must be 16 bytes");
        }
        return Arrays.copyOf(key, key.length);
    }

    private static byte[] prepareIV(final byte[] iv) {
        if (iv == null) {
            throw new IllegalArgumentException("IV must not be null");
        } else if (iv.length != 12) {
            throw new IllegalArgumentException("IV must be 12 bytes");
        }
        return Arrays.copyOf(iv, iv.length);
    }

    private final byte[] myKey;
    private final byte[] otherKey;
    private final byte[] myIV;
    private final byte[] otherIV;

    public AEAD(final byte[] myKey, final byte[] otherKey, final byte[] myIV, final byte[] otherIV) {
        this.myKey = prepareKey(myKey);
        this.otherKey = prepareKey(otherKey);
        this.myIV = prepareIV(myIV);
        this.otherIV = prepareIV(otherIV);
    }

    public byte[] open(final byte[] src, final long packetNumber, final byte[] aad) throws GeneralSecurityException {
        return process(src, packetNumber, aad, otherKey, otherIV, Cipher.DECRYPT_MODE);
    }

    public byte[] seal(final byte[] src, final long packetNumber, final byte[] aad) throws GeneralSecurityException {
        return process(src, packetNumber, aad, myKey, myIV, Cipher.ENCRYPT_MODE);
    }

    private final ThreadLocal<Cipher> ciphers = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        } catch (final GeneralSecurityException shouldNeverHappen) {
            throw new RuntimeException(shouldNeverHappen);
        }
    });

    private byte[] process(final byte[] src,
                           final long packetNumber,
                           final byte[] aad,
                           final byte[] key,
                           final byte[] iv,
                           final int mode) throws GeneralSecurityException {
        final Cipher cipher = ciphers.get();
        final SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        byte[] nonce = makeNonce(iv, packetNumber);
        final GCMParameterSpec spec = new GCMParameterSpec(128, nonce);

        String msg = mode == Cipher.DECRYPT_MODE ? "opening" : "sealing";
        System.out.println(msg + " src=" +
                Hex.hex(src) + " pn≈" + packetNumber + " aad=" +
                Hex.hex(aad) + " myKey=" +
                Hex.hex(myKey) + " otherKey=" +
                Hex.hex(otherKey) + " myIV=" +
                Hex.hex(myIV) + " otherIV=" +
                Hex.hex(otherIV));

        cipher.init(mode, secretKey, spec);
        cipher.updateAAD(aad);
        byte[] b= cipher.doFinal(src);
        msg = mode == Cipher.DECRYPT_MODE ? "opened" : "sealed";
        System.out.println(msg + " " + Hex.hex(b));
        return b;
    }

    public byte[] getMyKey() {
        return myKey;
    }

    public byte[] getOtherKey() {
        return otherKey;
    }

    public byte[] getMyIV() {
        return myIV;
    }

    public byte[] getOtherIV() {
        return otherIV;
    }

    @Override
    public String toString() {
        return "AEAD{" +
                "myKey=" + Hex.hex(myKey) +
                ", otherKey=" + Hex.hex(otherKey) +
                ", myIV=" + Hex.hex(myIV) +
                ", otherIV=" + Hex.hex(otherIV) +
                '}';
    }
}
