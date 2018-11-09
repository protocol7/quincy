package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.SortedMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ExtensionTest {

    @Test
    public void parseKnown() {
        byte[] b = Hex.dehex("000000150013000010717569632e636c656d656e74652e696f000500050100000000000a000a0008001d001700180019000b00020100000d001800160804040104030805050105030806060106030201020300320012001004010403050105030601060302010203ff0100010000120000003300260024001d0020a62c058352d7a007efbf4b944fad2dbcdf80b6ab56504533fc04a360a06dcc20002b00030203040ff5004200000000003c0000000400008000000a000400008000000b000400008000000100040000c00000020002006400080002006400030002001e0005000205ac00090000");

        Map<ExtensionType, Extension> ext = Extension.parseAll(Unpooled.wrappedBuffer(b));

        System.out.println(ext);

        assertRaw("0024001d0020a62c058352d7a007efbf4b944fad2dbcdf80b6ab56504533fc04a360a06dcc20", ext, ExtensionType.key_share);
        assertRaw("001004010403050105030601060302010203", ext, ExtensionType.signature_algorithms_cert);
        assertRaw("020304", ext, ExtensionType.supported_versions);
        assertRaw("", ext, ExtensionType.signed_certificate_timestamp);
        assertRaw("001608040401040308050501050308060601060302010203", ext, ExtensionType.signature_algorithms);
        assertRaw("0100", ext, ExtensionType.ec_point_formats);
        assertRaw("0008001d001700180019", ext, ExtensionType.supported_groups);
        assertRaw("0100000000", ext, ExtensionType.status_request);

        TransportParameters tps = (TransportParameters) ext.get(ExtensionType.QUIC);
        assertEquals(-1, tps.getAckDelayExponent());
        assertEquals(true, tps.isDisableMigration());
        assertEquals(30, tps.getIdleTimeout());
        assertEquals(100, tps.getInitialMaxBidiStreams());
        assertEquals(49152, tps.getInitialMaxData());
        assertEquals(32768, tps.getInitialMaxStreamDataBidiLocal());
        assertEquals(32768, tps.getInitialMaxStreamDataBidiRemote());
        assertEquals(32768, tps.getInitialMaxStreamDataUni());
        assertEquals(100, tps.getInitialMaxUniStreams());
        assertEquals(-1, tps.getMaxAckDelay());
        assertEquals(1452, tps.getMaxPacketSize());
        assertEquals(0, tps.getStatelessResetToken().length);
        assertEquals(0, tps.getOriginalConnectionId().length);
    }

    private void assertRaw(String hex, Map<ExtensionType, Extension> ext, ExtensionType type) {
        assertArrayEquals(Hex.dehex(hex), ((RawExtension)ext.get(type)).getData());
    }


}