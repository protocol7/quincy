package com.protocol7.quincy.tls.aead;

import com.protocol7.quincy.utils.Hex;

public class TestAEAD {

  public static AEAD create() {
    final byte[] key = Hex.dehex("f2b91f8858998f9a4866f9738cfd2392");
    final byte[] iv = Hex.dehex("b060c60e9b71df30636211c7");

    return new AEAD(key, key, iv, iv, key, key);
  }
}
