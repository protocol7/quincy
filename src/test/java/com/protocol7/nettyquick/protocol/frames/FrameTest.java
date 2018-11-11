package com.protocol7.nettyquick.protocol.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FrameTest {

    @Test
    public void cryptoframe() {
        CryptoFrame frame = new CryptoFrame(0, "hello".getBytes());
        ByteBuf bb = Unpooled.buffer();
        frame.write(bb);

        Frame parsed = Frame.parse(bb);
        assertTrue(parsed instanceof CryptoFrame);
    }
}