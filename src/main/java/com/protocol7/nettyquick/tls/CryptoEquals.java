package com.protocol7.nettyquick.tls;

import java.security.MessageDigest;

public class CryptoEquals {

    public static boolean isEqual(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
