package com.protocol7.nettyquick.tls;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.tls.extensions.Extension;
import com.protocol7.nettyquick.tls.extensions.SupportedVersions;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ServerHelloTest {

    @Test
    public void parseKnown() {
        byte[] sh = Hex.dehex("0200005603037cdef17464db2589d38fd069fd8e593fd7deda108bb84e12720212c47b74f96100130100002e002b0002030400330024001d0020bc3dd7c4c45142be87d00e1b3dd1a02d43b0be4ab41b71e1e6dfbea39c385417");
        ByteBuf bb = Unpooled.wrappedBuffer(sh);

        ServerHello hello = ServerHello.parse(bb);

        assertEquals("7cdef17464db2589d38fd069fd8e593fd7deda108bb84e12720212c47b74f961", Hex.hex(hello.getServerRandom()));
        assertEquals("", Hex.hex(hello.getSessionId()));
        assertEquals("1301", Hex.hex(hello.getCipherSuites()));

        assertEquals(2, hello.getExtensions().size());
    }

    @Test
    public void roundtrip() {
        List<Extension> ext = ImmutableList.of(SupportedVersions.TLS13);
        ServerHello sh = new ServerHello(Rnd.rndBytes(32), new byte[0], Hex.dehex("1301"), ext);

        ByteBuf bb = Unpooled.buffer();

        sh.write(bb);

        Bytes.debug(bb);

        ServerHello parsed = ServerHello.parse(bb);

        assertArrayEquals(sh.getServerRandom(), parsed.getServerRandom());
        assertArrayEquals(sh.getSessionId(), parsed.getSessionId());
        assertArrayEquals(sh.getCipherSuites(), parsed.getCipherSuites());

        assertEquals(ext, parsed.getExtensions());

    }
}