package com.protocol7.nettyquick.tls.aead;

import com.protocol7.nettyquick.tls.HKDFUtil;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.HandshakeAEAD;
import org.junit.Test;

import static com.protocol7.nettyquick.utils.Hex.dehex;
import static com.protocol7.nettyquick.utils.Hex.hex;
import static org.junit.Assert.assertEquals;

public class HandshakeAEADTest {

    @Test
    public void testKnownTls() {
        byte[] sharedSecret = dehex("df4a291baa1eb7cfa6934b29b474baad2697e29f1f920dcc77c8a0a088447624");
        byte[] helloHash = dehex("da75ce1139ac80dae4044da932350cf65c97ccc9e33f1e6f7d2d4b18b736ffd5");

        byte[] handshakeSecret = HKDFUtil.calculateHandshakeSecret(sharedSecret);

        AEAD aead = HandshakeAEAD.create(handshakeSecret, helloHash, false, true);

        assertEquals("7154f314e6be7dc008df2c832baa1d39", hex(aead.getMyKey()));
        assertEquals("844780a7acad9f980fa25c114e43402a", hex(aead.getOtherKey()));
        assertEquals("71abc2cae4c699d47c600268", hex(aead.getMyIV()));
        assertEquals("4c042ddc120a38d1417fc815", hex(aead.getOtherIV()));
    }

    @Test
    public void testKnownQuic() {
        byte[] sharedSecret = dehex("c1a6e992dc90a4729325da67fc4f90d7ec853ca2481ae9a7bca6cd33eff8403c");
        byte[] helloHash = dehex("96e0581a2ce8cb7154fd942ed2f2cd37861783fc8498d02e2b533d8ed927e27a");

        byte[] handshakeSecret = HKDFUtil.calculateHandshakeSecret(sharedSecret);

        AEAD aead = HandshakeAEAD.create(handshakeSecret, helloHash, true, true);

        assertEquals("106b616bfe8f1a7edca6f95321f6dbe4", hex(aead.getMyKey()));
        assertEquals("e9ed76aa63fb170912bacd53329b8c20", hex(aead.getOtherKey()));
        assertEquals("1c28f3edb0df229068c6e48f", hex(aead.getMyIV()));
        assertEquals("f9dcc8ae4149ecd2985f9e96", hex(aead.getOtherIV()));
    }
}