package com.protocol7.nettyquick.tls;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.protocol7.nettyquick.utils.Bytes;

public class Hash {

    private static final HashFunction SHA256 = Hashing.sha256();

    public static byte[] sha256(byte[]... data) {
        return SHA256.hashBytes(Bytes.concat(data)).asBytes();
    }
}
