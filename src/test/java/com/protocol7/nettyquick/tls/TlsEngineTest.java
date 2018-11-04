package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class TlsEngineTest {

    @Test
    public void clientHelloFromGo() {
        byte[] ch = Hex.dehex("010001470303a76cc637036e871b63c463a73175ca81a5f09b14e80f58715d52c8f5e90a794a10fe8a3d447a7ea799dfd5bdbffca6e5bc0026130113021303c02fc030c02bc02ccca8cca9c013c009c014c00a009c009d002f0035c012000a010000e8000000150013000010717569632e636c656d656e74652e696f000500050100000000000a000a0008001d001700180019000b00020100000d001800160804040104030805050105030806060106030201020300320012001004010403050105030601060302010203ff0100010000120000003300260024001d0020a62c058352d7a007efbf4b944fad2dbcdf80b6ab56504533fc04a360a06dcc20002b00030203040ff5004200000000003c0000000400008000000a000400008000000b000400008000000100040000c00000020002006400080002006400030002001e0005000205ac00090000");

        parseClentHello(ch);
    }

    @Test
    public void clientHello() {
        TlsEngine engine = new TlsEngine(true);

        byte[] ch = engine.start();

        parseClentHello(ch);

    }

    private void parseClentHello(byte[] ch) {
        ByteBuf bb = Unpooled.wrappedBuffer(ch);


        Bytes.debug(bb);

        assertNextBytes(bb, "01"); // HandshakeType
        debug(bb, 1);

        byte[] b = new byte[3];
        bb.readBytes(b);
        int payloadLength = b[0] << 16 | b[1] << 8 | b[2];
        assertEquals(payloadLength, bb.readableBytes());
        debug(bb, 4);

        assertNextBytes(bb, "03 03"); // Client version

        debug(bb, 6);

        b = new byte[32];
        bb.readBytes(b); // client random
        debug(bb, 38);

        int sessionIdLen = bb.readByte();
        b = new byte[sessionIdLen];
        bb.readBytes(b); // session ID
        debug(bb, 38 + 1 + sessionIdLen);

        int cipherSuiteLen = bb.readShort();
        b = new byte[cipherSuiteLen];
        bb.readBytes(b); // cipher suites
        debug(bb, 38 + 1 + sessionIdLen + 2 + cipherSuiteLen);

        assertNextBytes(bb, "01 00"); // compression disabled

        debug(bb, 38 + 1 + sessionIdLen + 2 + cipherSuiteLen + 2);

        int extensionsLen = bb.readShort();
        b = new byte[extensionsLen];
        bb.readBytes(b); // extensions
        debug(bb, 38 + 1 + sessionIdLen + 2 + cipherSuiteLen + 2 + extensionsLen);

        parseExtensions(b);
    }

    private void parseExtensions(byte[] ext) {
        ByteBuf bb = Unpooled.wrappedBuffer(ext);

        while (bb.isReadable()) {
            byte[] type = new byte[2];
            bb.readBytes(type);
            int len = bb.readShort();
            byte[] data = new byte[len];
            bb.readBytes(data);

            System.out.println(extensionType(type) + ": " + Hex.hex(data));

            if (Arrays.equals(type, new byte[]{0x0f, (byte)0xf5})) {
                parseTransportParameters(data);
            }
        }
    }

    private String extensionType(byte[] type) {
        try {
            int t = type[0] << 8 | type[1];
            return ExtensionType.fromValue(t).name();
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