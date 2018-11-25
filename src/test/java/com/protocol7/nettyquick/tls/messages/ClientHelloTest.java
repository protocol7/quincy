package com.protocol7.nettyquick.tls.messages;

import static com.protocol7.nettyquick.TestUtil.assertHex;
import static com.protocol7.nettyquick.tls.CipherSuite.TLS_AES_128_GCM_SHA256;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.tls.Group;
import com.protocol7.nettyquick.tls.KeyExchange;
import com.protocol7.nettyquick.tls.extensions.*;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ClientHelloTest {

  @Test
  public void inverseRoundtrip() {
    byte[] ch =
        Hex.dehex(
            "010000a903039b2dced3fa6d25973ccc18315e4df1cbd0bef7e587ed8472e9dca4686557a70300000213010100007e003300260024001d00203318def2702c981b7efc29c7dc78c383caefc80b12854272e7596a5eb6079236000a00040002001d002b00030203040ff5004100000000003b0000000480008000000100048000c000000200024064000300011e0005000245ac00080002406400090000000a000480008000000b000480008000");

    ClientHello hello = ClientHello.parse(ch);

    ByteBuf bb = Unpooled.buffer();
    hello.write(bb);

    byte[] written = new byte[bb.writerIndex()];
    bb.readBytes(written);

    assertHex(ch, written);
  }

  @Test
  public void parseJavaKnown() {
    byte[] ch =
        Hex.dehex(
            "0100010603036ec8ad3d54ac887561d563e04b6f09d24f2e649be15c9164df95654e6e3fa4ba20a29ad438fded33463aa6c107ec92ce0fd112a1256fbcd9edf265ee82cab45b7800021301010000bb000500050100000000000a0012001000170018001901000101010201030104000d001e001c0403050306030804080508060809080a080b040105010601020302010032001e001c0403050306030804080508060809080a080b04010501060102030201002b0003020304002d00020101003300470045001700410495c5ca888d7ff56fc86a73bee197d37477c6e87ff7d5e98d8f5b56498a2f8c03e9b1d8a0ee1c7e6f23be5cd3affe43e8589f7cd2d34eadaf67dec58fbe0f6f55");

    ClientHello hello = ClientHello.parse(ch);

    assertHex(
        "6ec8ad3d54ac887561d563e04b6f09d24f2e649be15c9164df95654e6e3fa4ba",
        hello.getClientRandom());
    assertHex(
        "a29ad438fded33463aa6c107ec92ce0fd112a1256fbcd9edf265ee82cab45b78", hello.getSessionId());
    assertEquals(ImmutableList.of(TLS_AES_128_GCM_SHA256), hello.getCipherSuites());
    assertEquals(7, hello.getExtensions().size());
  }

  @Test
  public void defaults() {
    KeyExchange kek = KeyExchange.generate(Group.X25519);
    ClientHello ch = ClientHello.defaults(kek, TransportParameters.defaults());

    assertEquals(32, ch.getClientRandom().length);
    assertEquals(0, ch.getSessionId().length);
    assertEquals(ImmutableList.of(TLS_AES_128_GCM_SHA256), ch.getCipherSuites());

    KeyShare keyShare = (KeyShare) ch.geExtension(ExtensionType.key_share).get();
    assertEquals(1, keyShare.getKeys().size());
    assertEquals(Hex.hex(kek.getPublicKey()), Hex.hex(keyShare.getKey(Group.X25519).get()));

    SupportedGroups supportedGroups =
        (SupportedGroups) ch.geExtension(ExtensionType.supported_groups).get();
    assertEquals(ImmutableList.of(Group.X25519), supportedGroups.getGroups());

    SupportedVersions supportedVersions =
        (SupportedVersions) ch.geExtension(ExtensionType.supported_versions).get();
    assertEquals("0304", Hex.hex(supportedVersions.getVersion()));

    TransportParameters transportParameters =
        (TransportParameters) ch.geExtension(ExtensionType.QUIC).get();
    assertEquals(TransportParameters.defaults(), transportParameters);
  }
}
