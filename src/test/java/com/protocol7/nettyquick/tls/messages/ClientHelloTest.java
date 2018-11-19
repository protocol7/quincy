package com.protocol7.nettyquick.tls.messages;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.tls.Group;
import com.protocol7.nettyquick.tls.KeyExchange;
import com.protocol7.nettyquick.tls.extensions.*;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClientHelloTest {

    @Test
    public void parseGoKnown() {
        byte[] ch = Hex.dehex("010001470303a76cc637036e871b63c463a73175ca81a5f09b14e80f58715d52c8f5e90a794a10fe8a3d447a7ea799dfd5bdbffca6e5bc0026130113021303c02fc030c02bc02ccca8cca9c013c009c014c00a009c009d002f0035c012000a010000e8000000150013000010717569632e636c656d656e74652e696f000500050100000000000a000a0008001d001700180019000b00020100000d001800160804040104030805050105030806060106030201020300320012001004010403050105030601060302010203ff0100010000120000003300260024001d0020a62c058352d7a007efbf4b944fad2dbcdf80b6ab56504533fc04a360a06dcc20002b00030203040ff5004200000000003c0000000400008000000a000400008000000b000400008000000100040000c00000020002006400080002006400030002001e0005000205ac00090000");

        ClientHello hello = ClientHello.parse(ch);

        assertEquals("a76cc637036e871b63c463a73175ca81a5f09b14e80f58715d52c8f5e90a794a",
                Hex.hex(hello.getClientRandom()));
        assertEquals("fe8a3d447a7ea799dfd5bdbffca6e5bc",
                Hex.hex(hello.getSessionId()));
        assertEquals("130113021303c02fc030c02bc02ccca8cca9c013c009c014c00a009c009d002f0035c012000a",
                Hex.hex(hello.getCipherSuites()));

        assertEquals(11, hello.getExtensions().size());
    }

    @Test
    public void inverseRoundtrip() {
        byte[] ch = Hex.dehex("010001410303a76cc637036e871b63c463a73175ca81a5f09b14e80f58715d52c8f5e90a794a10fe8a3d447a7ea799dfd5bdbffca6e5bc0026130113021303c02fc030c02bc02ccca8cca9c013c009c014c00a009c009d002f0035c012000a010000e20ff5004200000000003c0000000400008000000100040000c00000020002006400030002001e0005000205ac00080002006400090000000a000400008000000b000400008000003300260024001d0020a62c058352d7a007efbf4b944fad2dbcdf80b6ab56504533fc04a360a06dcc2000320012001004010403050105030601060302010203002b000302030400120000000d0018001608040401040308050501050308060601060302010203000b00020100000a00040002001d000500050100000000000000150013000010717569632e636c656d656e74652e696fff01000100");

        ClientHello hello = ClientHello.parse(ch);

        ByteBuf bb = Unpooled.buffer();
        hello.write(bb);

        byte[] written = new byte[bb.writerIndex()];
        bb.readBytes(written);

        Assert.assertEquals(Hex.hex(ch), Hex.hex(written));
    }

    @Test
    public void parseJavaKnown() {
        byte[] ch = Hex.dehex("0100010603036ec8ad3d54ac887561d563e04b6f09d24f2e649be15c9164df95654e6e3fa4ba20a29ad438fded33463aa6c107ec92ce0fd112a1256fbcd9edf265ee82cab45b7800021301010000bb000500050100000000000a0012001000170018001901000101010201030104000d001e001c0403050306030804080508060809080a080b040105010601020302010032001e001c0403050306030804080508060809080a080b04010501060102030201002b0003020304002d00020101003300470045001700410495c5ca888d7ff56fc86a73bee197d37477c6e87ff7d5e98d8f5b56498a2f8c03e9b1d8a0ee1c7e6f23be5cd3affe43e8589f7cd2d34eadaf67dec58fbe0f6f55");

        ClientHello hello = ClientHello.parse(ch);

        assertEquals("6ec8ad3d54ac887561d563e04b6f09d24f2e649be15c9164df95654e6e3fa4ba",
                Hex.hex(hello.getClientRandom()));
        assertEquals("a29ad438fded33463aa6c107ec92ce0fd112a1256fbcd9edf265ee82cab45b78",
                Hex.hex(hello.getSessionId()));
        assertEquals("1301",
                Hex.hex(hello.getCipherSuites()));
        assertEquals(7, hello.getExtensions().size());
    }

    @Test
    public void defaults() {
        KeyExchange kek = KeyExchange.generate(Group.X25519);
        ClientHello ch = ClientHello.defaults(kek, TransportParameters.defaults());

        assertEquals(32, ch.getClientRandom().length);
        assertEquals(0, ch.getSessionId().length);
        assertEquals("1301", Hex.hex(ch.getCipherSuites()));

        KeyShare keyShare = (KeyShare) ch.geExtension(ExtensionType.key_share).get();
        assertEquals(1, keyShare.getKeys().size());
        assertEquals(Hex.hex(kek.getPublicKey()), Hex.hex(keyShare.getKey(Group.X25519).get()));

        SupportedGroups supportedGroups = (SupportedGroups) ch.geExtension(ExtensionType.supported_groups).get();
        assertEquals(ImmutableList.of(Group.X25519), supportedGroups.getGroups());

        SupportedVersions supportedVersions = (SupportedVersions) ch.geExtension(ExtensionType.supported_versions).get();
        assertEquals("0304", Hex.hex(supportedVersions.getVersion()));

        TransportParameters transportParameters = (TransportParameters) ch.geExtension(ExtensionType.QUIC).get();
        assertEquals(TransportParameters.defaults(), transportParameters);
    }
}