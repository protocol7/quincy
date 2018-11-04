package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.utils.Hex;

public class TestAEAD {

    public static AEAD create() {
        byte[] key = Hex.dehex("f2b91f8858998f9a4866f9738cfd2392");
        byte[] iv = Hex.dehex("b060c60e9b71df30636211c7");

        return new AEAD(key, key, iv, iv);
    }


}
