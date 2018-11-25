package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

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