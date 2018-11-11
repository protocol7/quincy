package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.tls.extensions.ExtensionType;
import com.protocol7.nettyquick.tls.extensions.TransportParameters;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang.StringUtils;

import static org.junit.Assert.assertEquals;

public class ClientTlsEngineTest {

    protected static int read24(ByteBuf bb) {
        byte[] b = new byte[3];
        bb.readBytes(b);
        return (b[0] & 0xFF) << 16 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF);
    }

    private String extensionType(byte[] type) {
        try {
            int t = type[0] << 8 | type[1];
            return ExtensionType.fromValue(t).getName();
        } catch (IllegalArgumentException e) {
            return "Unknown (" + Hex.hex(type) + ")";
        }
    }

    private void parseTransportParameters(byte[] data) {
        ByteBuf bb = Unpooled.wrappedBuffer(data);

        TransportParameters tps = TransportParameters.parse(bb);

        System.out.println(tps);
    }

    private void debug(ByteBuf bb, int offset) {
        Bytes.debug(StringUtils.repeat("  ", offset), bb);
    }

    private void assertNextBytes(ByteBuf actual, String expectedStr) {
        assertNextBytes(actual, Hex.dehex(expectedStr));
    }

    private void assertNextBytes(ByteBuf actual, byte[] expected) {
        byte[] b = new byte[expected.length];
        actual.readBytes(b);

        assertEquals(Hex.hex(expected), Hex.hex(b));
    }

}