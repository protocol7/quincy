package com.protocol7.quincy.tls.messages;

import static com.protocol7.quincy.tls.CipherSuite.TLS_AES_128_GCM_SHA256;
import static com.protocol7.quincy.tls.TestUtil.assertHex;
import static com.protocol7.quincy.utils.Hex.dehex;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.tls.Group;
import com.protocol7.quincy.tls.KeyExchange;
import com.protocol7.quincy.tls.TestUtil;
import com.protocol7.quincy.tls.extensions.ExtensionType;
import com.protocol7.quincy.tls.extensions.KeyShare;
import com.protocol7.quincy.tls.extensions.SupportedGroups;
import com.protocol7.quincy.tls.extensions.SupportedVersion;
import com.protocol7.quincy.tls.extensions.SupportedVersions;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;

public class ClientHelloTest {

  @Test
  public void inverseRoundtrip() {
    final byte[] ch =
        dehex(
            "010000a90303db87599ea479c556a9d68a35cab2068ebc4775ec6d16a05fe83fbcf0f8f2fe7f00000213010100007e003300260024001d00202ed502c91482268eeef791cf253e90319a19230bf4a8ca2ac3dc91c1d8cc5e57000a00040002001d002b00030203040ff5004100000000003b0005000480008000000400048000c000000800024064000100011e0003000245ac000900024064000c000000060004800080000007000480008000");

    final ClientHello hello = ClientHello.parse(Unpooled.wrappedBuffer(ch), false);

    final ByteBuf bb = Unpooled.buffer();
    hello.write(bb, true);

    final byte[] written = new byte[bb.writerIndex()];
    bb.readBytes(written);

    assertHex(ch, written);
  }

  @Test
  public void parseJavaKnown() {
    final ByteBuf ch =
        Unpooled.wrappedBuffer(
            dehex(
                "0100010603036ec8ad3d54ac887561d563e04b6f09d24f2e649be15c9164df95654e6e3fa4ba20a29ad438fded33463aa6c107ec92ce0fd112a1256fbcd9edf265ee82cab45b7800021301010000bb000500050100000000000a0012001000170018001901000101010201030104000d001e001c0403050306030804080508060809080a080b040105010601020302010032001e001c0403050306030804080508060809080a080b04010501060102030201002b0003020304002d00020101003300470045001700410495c5ca888d7ff56fc86a73bee197d37477c6e87ff7d5e98d8f5b56498a2f8c03e9b1d8a0ee1c7e6f23be5cd3affe43e8589f7cd2d34eadaf67dec58fbe0f6f55"));

    final ClientHello hello = ClientHello.parse(ch, false);

    assertHex(
        "6ec8ad3d54ac887561d563e04b6f09d24f2e649be15c9164df95654e6e3fa4ba",
        hello.getClientRandom());
    assertHex(
        "a29ad438fded33463aa6c107ec92ce0fd112a1256fbcd9edf265ee82cab45b78", hello.getSessionId());
    assertEquals(List.of(TLS_AES_128_GCM_SHA256), hello.getCipherSuites());
    assertEquals(7, hello.getExtensions().size());
  }

  @Test
  public void defaults() {
    final KeyExchange kek = KeyExchange.generate(Group.X25519);
    final ClientHello ch = ClientHello.defaults(kek, List.of(TestUtil.tps()));

    assertEquals(32, ch.getClientRandom().length);
    assertEquals(0, ch.getSessionId().length);
    assertEquals(List.of(TLS_AES_128_GCM_SHA256), ch.getCipherSuites());

    final KeyShare keyShare = (KeyShare) ch.getExtension(ExtensionType.KEY_SHARE).get();
    assertEquals(1, keyShare.getKeys().size());
    assertEquals(Hex.hex(kek.getPublicKey()), Hex.hex(keyShare.getKey(Group.X25519).get()));

    final SupportedGroups supportedGroups =
        (SupportedGroups) ch.getExtension(ExtensionType.SUPPORTED_GROUPS).get();
    assertEquals(List.of(Group.X25519), supportedGroups.getGroups());

    final SupportedVersions supportedVersions =
        (SupportedVersions) ch.getExtension(ExtensionType.SUPPORTED_VERSIONS).get();
    assertEquals(List.of(SupportedVersion.TLS13), supportedVersions.getVersions());

    final TransportParameters transportParameters =
        (TransportParameters) ch.getExtension(ExtensionType.QUIC).get();
    assertEquals(TestUtil.tps(), transportParameters);
  }
}
