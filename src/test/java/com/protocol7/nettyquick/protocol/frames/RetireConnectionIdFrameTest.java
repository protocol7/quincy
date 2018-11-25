package com.protocol7.nettyquick.protocol.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RetireConnectionIdFrameTest {


    @Test
    public void roundtrip() {
        RetireConnectionIdFrame rcif = new RetireConnectionIdFrame(123);

        ByteBuf bb = Unpooled.buffer();
        rcif.write(bb);

        RetireConnectionIdFrame parsed = RetireConnectionIdFrame.parse(bb);

        assertEquals(rcif.getSequenceNumber(), parsed.getSequenceNumber());

    }
}