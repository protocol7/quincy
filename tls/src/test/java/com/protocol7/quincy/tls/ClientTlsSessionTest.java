package com.protocol7.quincy.tls;

import static com.protocol7.quincy.tls.CipherSuite.TLS_AES_128_GCM_SHA256;
import static com.protocol7.quincy.utils.Hex.dehex;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.protocol7.quincy.tls.ClientTlsSession.CertificateInvalidException;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.extensions.Extension;
import com.protocol7.quincy.tls.extensions.ExtensionType;
import com.protocol7.quincy.tls.extensions.KeyShare;
import com.protocol7.quincy.tls.extensions.SupportedGroups;
import com.protocol7.quincy.tls.extensions.SupportedVersion;
import com.protocol7.quincy.tls.extensions.SupportedVersions;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import com.protocol7.quincy.tls.messages.ClientHello;
import com.protocol7.quincy.tls.messages.ServerHello;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ClientTlsSessionTest {

  private final byte[] connectionId = Rnd.rndBytes(16);
  private final ClientTlsSession engine =
      new ClientTlsSession(
          InitialAEAD.create(Rnd.rndBytes(4), true),
          new byte[0],
          TestUtil.tps(),
          new NoopCertificateValidator());
  private final ClientTlsSession started =
      new ClientTlsSession(
          InitialAEAD.create(Rnd.rndBytes(4), true),
          new byte[0],
          TestUtil.tps(),
          new NoopCertificateValidator());

  @Before
  public void setUp() {
    started.startHandshake(connectionId);
  }

  @Test
  public void handshake() {
    final ByteBuf ch = Unpooled.wrappedBuffer(engine.startHandshake(connectionId));

    final ClientHello hello = ClientHello.parse(ch, false);

    assertEquals(32, hello.getClientRandom().length);
    assertEquals(0, hello.getSessionId().length);
    assertEquals(List.of(TLS_AES_128_GCM_SHA256), hello.getCipherSuites());

    assertEquals(
        32,
        ((KeyShare) hello.getExtension(ExtensionType.KEY_SHARE).get())
            .getKey(Group.X25519)
            .get()
            .length);
    assertEquals(
        List.of(Group.X25519),
        ((SupportedGroups) hello.getExtension(ExtensionType.SUPPORTED_GROUPS).get()).getGroups());
    assertEquals(
        List.of(SupportedVersion.TLS13),
        ((SupportedVersions) hello.getExtension(ExtensionType.SUPPORTED_VERSIONS).get())
            .getVersions());

    final TransportParameters tps =
        (TransportParameters) hello.getExtension(ExtensionType.QUIC).get();
    final TransportParameters expectedTp =
        TransportParameters.newBuilder(TestUtil.tps())
            .withInitialSourceConnectionId(connectionId)
            .build();
    assertEquals(expectedTp, tps);
  }

  private KeyShare keyshare() {
    return KeyShare.of(Group.X25519, Rnd.rndBytes(32));
  }

  @Test
  public void serverHello() {
    final List<Extension> ext = List.of(keyshare(), SupportedVersions.TLS13, TestUtil.tps());

    final ByteBuf b = Unpooled.wrappedBuffer(sh(new byte[32], TLS_AES_128_GCM_SHA256, ext));

    assertFalse(started.available(EncryptionLevel.Handshake));

    started.handleServerHello(b);

    assertTrue(started.available(EncryptionLevel.Handshake));
  }

  private byte[] sh(
      final byte[] serverRandom, final CipherSuite cipherSuite, final List<Extension> ext) {
    final ServerHello sh = new ServerHello(serverRandom, new byte[0], cipherSuite, ext);
    final ByteBuf bb = Unpooled.buffer();
    sh.write(bb);
    return Bytes.drainToArray(bb);
  }

  private List<Extension> ext(final Extension... extensions) {
    return Arrays.asList(extensions);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serverHelloIllegalVersion() {
    final ByteBuf b =
        Unpooled.wrappedBuffer(
            dehex(
                "0200009c"
                    + "9999"
                    + "000000000000000000000000000000000000000000000000000000000000000000130100007400330024001d0020071967d323b1e8362ae9dfdb5280a220b4795019261715f54a6bfc251b17fc45002b000203040ff5004200000000003c0000000400008000000100040000c00000020002006400030002001e0005000205ac00080002006400090000000a000400008000000b000400008000"));

    started.handleServerHello(b);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serverHelloNoKeyShare() {
    final ByteBuf b =
        Unpooled.wrappedBuffer(
            sh(new byte[32], TLS_AES_128_GCM_SHA256, ext(SupportedVersions.TLS13, TestUtil.tps())));

    started.handleServerHello(b);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serverHelloNoSupportedVersion() {
    final ByteBuf b =
        Unpooled.wrappedBuffer(
            sh(new byte[32], TLS_AES_128_GCM_SHA256, ext(keyshare(), TestUtil.tps())));

    started.handleServerHello(b);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serverHelloIllegalSupportedVersion() {
    final ByteBuf b =
        Unpooled.wrappedBuffer(
            dehex(
                "0200009c0303000000000000000000000000000000000000000000000000000000000000000000130100007400330024001d0020071967d323b1e8362ae9dfdb5280a220b4795019261715f54a6bfc251b17fc45002b0002"
                    + "9999"
                    + "0ff5004200000000003c0000000400008000000100040000c00000020002006400030002001e0005000205ac00080002006400090000000a000400008000000b000400008000"));

    started.handleServerHello(b);
  }

  @Test(expected = IllegalStateException.class)
  public void serverHelloWithoutStart() {
    engine.handleServerHello(Unpooled.wrappedBuffer(new byte[0]));
  }

  @Test(expected = IllegalStateException.class)
  public void serverHandshakeWithoutStart() throws CertificateInvalidException {
    engine.handleHandshake(new byte[0], 0);
  }
}
