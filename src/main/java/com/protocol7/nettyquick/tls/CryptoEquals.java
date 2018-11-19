package com.protocol7.nettyquick.tls;

public class CryptoEquals {

    // from https://codahale.com/a-lesson-in-timing-attacks/
    public static boolean isEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
