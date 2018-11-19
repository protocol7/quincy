package com.protocol7.nettyquick.tls.extensions;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static com.protocol7.nettyquick.tls.Group.X25519;
import static com.protocol7.nettyquick.utils.Hex.hex;
import static org.junit.Assert.assertEquals;

public class ExtensionTest {

    @Test
    public void parseKnown() {
        byte[] b = Hex.dehex("000000150013000010717569632e636c656d656e74652e696f000500050100000000000a000a0008001d001700180019000b00020100000d001800160804040104030805050105030806060106030201020300320012001004010403050105030601060302010203ff0100010000120000003300260024001d0020a62c058352d7a007efbf4b944fad2dbcdf80b6ab56504533fc04a360a06dcc20002b00030203040ff5004200000000003c0000000400008000000a000400008000000b000400008000000100040000c00000020002006400080002006400030002001e0005000205ac00090000");

        List<Extension> ext = Extension.parseAll(Unpooled.wrappedBuffer(b), true);

        Iterator<Extension> iter = ext.iterator();

        assertRaw("0013000010717569632e636c656d656e74652e696f", ExtensionType.server_name, iter.next());
        assertRaw("0100000000", ExtensionType.status_request, iter.next());

        SupportedGroups supportedGroups = (SupportedGroups) iter.next();
        assertEquals(ImmutableList.of(X25519), supportedGroups.getGroups());

        assertRaw("0100", ExtensionType.ec_point_formats, iter.next());
        assertRaw("001608040401040308050501050308060601060302010203", ExtensionType.signature_algorithms, iter.next());
        assertRaw("001004010403050105030601060302010203", ExtensionType.signature_algorithms_cert, iter.next());
        assertRaw("00", ExtensionType.fromValue(-255), iter.next());
        assertRaw("", ExtensionType.signed_certificate_timestamp, iter.next());

        KeyShare keyShare = (KeyShare) iter.next();
        assertEquals(1, keyShare.getKeys().size());
        assertEquals("a62c058352d7a007efbf4b944fad2dbcdf80b6ab56504533fc04a360a06dcc20", hex(keyShare.getKey(X25519).get()));

        SupportedVersions supportedVersions = (SupportedVersions) iter.next();
        assertEquals("0304", hex(supportedVersions.getVersion()));

        TransportParameters tps = (TransportParameters) iter.next();
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

    private void assertRaw(String actual, ExtensionType type, Extension ext) {
        assertEquals(type, ext.getType());
        assertEquals(actual, hex(((RawExtension)ext).getData()));
    }
}