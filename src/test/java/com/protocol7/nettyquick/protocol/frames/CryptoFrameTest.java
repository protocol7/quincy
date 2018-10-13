package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.protocol.Varint;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class CryptoFrameTest {

    @Test
    public void roundtrip() {
        CryptoFrame frame = new CryptoFrame(new Varint(123), Hex.dehex("1234"));

        ByteBuf bb = Unpooled.buffer();
        frame.write(bb);

        CryptoFrame parsed = CryptoFrame.parse(bb);

        assertEquals(parsed.getOffset(), frame.getOffset());
        assertArrayEquals(parsed.getCryptoData(), frame.getCryptoData());
    }
}