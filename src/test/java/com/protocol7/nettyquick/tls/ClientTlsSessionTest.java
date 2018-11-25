package com.protocol7.nettyquick.tls;

import static com.protocol7.nettyquick.tls.CipherSuite.TLS_AES_128_GCM_SHA256;
import static com.protocol7.nettyquick.utils.Hex.dehex;
import static com.protocol7.nettyquick.utils.Hex.hex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.extensions.*;
import com.protocol7.nettyquick.tls.messages.ClientHello;
import com.protocol7.nettyquick.tls.messages.ServerHello;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ClientTlsSessionTest {

  private final ClientTlsSession engine = new ClientTlsSession();
  private final ClientTlsSession started = new ClientTlsSession();

  @Before
  public void setUp() {
    started.start();
  }

  @Test
  public void handshake() {
    byte[] ch = engine.start();

    ClientHello hello = ClientHello.parse(ch);

    assertEquals(32, hello.getClientRandom().length);
    assertEquals(0, hello.getSessionId().length);
    assertEquals(ImmutableList.of(TLS_AES_128_GCM_SHA256), hello.getCipherSuites());

    assertEquals(
        32,
        ((KeyShare) hello.geExtension(ExtensionType.key_share).get())
            .getKey(Group.X25519)
            .get()
            .length);
    assertEquals(
        ImmutableList.of(Group.X25519),
        ((SupportedGroups) hello.geExtension(ExtensionType.supported_groups).get()).getGroups());
    assertEquals(
        "0304",
        hex(
            ((SupportedVersions) hello.geExtension(ExtensionType.supported_versions).get())
                .getVersion()));

    TransportParameters tps = (TransportParameters) hello.geExtension(ExtensionType.QUIC).get();
    assertEquals(TransportParameters.defaults(), tps);
  }

  private KeyShare keyshare() {
    return KeyShare.of(Group.X25519, Rnd.rndBytes(32));
  }

  @Test
  public void serverHello() {
    List<Extension> ext =
        ImmutableList.of(keyshare(), SupportedVersions.TLS13, TransportParameters.defaults());

    byte[] b = sh(new byte[32], TLS_AES_128_GCM_SHA256, ext);

    AEAD aead = started.handleServerHello(b);

    assertNotNull(aead);
    // TODO mock random and test AEAD keys
  }

  private byte[] sh(byte[] serverRandom, CipherSuite cipherSuite, List<Extension> ext) {
    ServerHello sh = new ServerHello(serverRandom, new byte[0], cipherSuite, ext);
    ByteBuf bb = Unpooled.buffer();
    sh.write(bb);
    return Bytes.drainToArray(bb);
  }

  private List<Extension> ext(Extension... extensions) {
    return Arrays.asList(extensions);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serverHelloIllegalVersion() {
    byte[] b =
        dehex(
            "0200009c"
                + "9999"
                + "000000000000000000000000000000000000000000000000000000000000000000130100007400330024001d0020071967d323b1e8362ae9dfdb5280a220b4795019261715f54a6bfc251b17fc45002b000203040ff5004200000000003c0000000400008000000100040000c00000020002006400030002001e0005000205ac00080002006400090000000a000400008000000b000400008000");

    started.handleServerHello(b);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serverHelloNoKeyShare() {
    byte[] b =
        sh(
            new byte[32],
            TLS_AES_128_GCM_SHA256,
            ext(SupportedVersions.TLS13, TransportParameters.defaults()));

    started.handleServerHello(b);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serverHelloNoSupportedVersion() {
    byte[] b =
        sh(new byte[32], TLS_AES_128_GCM_SHA256, ext(keyshare(), TransportParameters.defaults()));

    started.handleServerHello(b);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serverHelloIllegalSupportedVersion() {
    byte[] b =
        dehex(
            "0200009c0303000000000000000000000000000000000000000000000000000000000000000000130100007400330024001d0020071967d323b1e8362ae9dfdb5280a220b4795019261715f54a6bfc251b17fc45002b0002"
                + "9999"
                + "0ff5004200000000003c0000000400008000000100040000c00000020002006400030002001e0005000205ac00080002006400090000000a000400008000000b000400008000");

    started.handleServerHello(b);
  }

  @Test(expected = IllegalStateException.class)
  public void serverHelloWithoutStart() {
    engine.handleServerHello(new byte[0]);
  }

  @Test(expected = IllegalStateException.class)
  public void serverHandshakeWithoutStart() {
    engine.handleHandshake(new byte[0]);
  }
}
