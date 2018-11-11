package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

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
}