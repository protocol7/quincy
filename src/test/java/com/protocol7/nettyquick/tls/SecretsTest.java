package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.utils.Hex;
import org.junit.Assert;
import org.junit.Test;

public class SecretsTest {


    @Test
    public void testSecretsGenerator() {
        ConnectionId connId = new ConnectionId(Hex.dehex("c574992996084bbe8e02b3cbb91fdd"));

        AEAD aead = NullAEAD.create(connId, true);

        Assert.assertEquals("bfa010b9339b3f09221521169d826362", Hex.hex(aead.getMyKey()));
        Assert.assertEquals("c3d413881123d5ba92583c5beb798ecf", Hex.hex(aead.getOtherKey()));
        Assert.assertEquals("f7daa3324ffce70f7c4cce9b", Hex.hex(aead.getMyIV()));
        Assert.assertEquals("d0844438e77ac16643d4080a", Hex.hex(aead.getOtherIV()));
    }
}
