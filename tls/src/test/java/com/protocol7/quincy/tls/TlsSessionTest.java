package com.protocol7.quincy.tls;

import static com.protocol7.quincy.utils.Hex.dehex;

import com.protocol7.quincy.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.messages.ServerHandshake;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.security.PrivateKey;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TlsSessionTest {

  private final byte[] version = dehex("51474fff");

  private PrivateKey privateKey;
  private final ClientTlsSession client =
      new ClientTlsSession(InitialAEAD.create(Rnd.rndBytes(4), true), TestUtil.tps(version));
  private ServerTlsSession server;

  @Before
  public void setUp() throws Exception {
    privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");
    byte[] serverCert = KeyUtil.getCertFromCrt("src/test/resources/server.crt").getEncoded();

    server =
        new ServerTlsSession(
            InitialAEAD.create(Rnd.rndBytes(4), false),
            TestUtil.tps(version),
            List.of(serverCert),
            privateKey);
  }

  @Test
  public void handshake() {
    byte[] clientHello = client.startHandshake();

    ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());
    byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get().getFin();

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeWithInvalidServerCertVerification() {
    byte[] clientHello = client.startHandshake();

    ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());

    ByteBuf bb = Unpooled.wrappedBuffer(shah.getServerHandshake());
    ServerHandshake handshake = ServerHandshake.parse(bb, true);

    byte[] sig = handshake.getServerCertificateVerify().getSignature();

    sig[0]++; // modify signature

    byte[] scv =
        Bytes.write(
            handshake.getEncryptedExtensions(),
            handshake.getServerCertificate(),
            new ServerHandshake.ServerCertificateVerify(
                handshake.getServerCertificateVerify().getType(), sig),
            handshake.getServerHandshakeFinished());

    byte[] clientFin = client.handleHandshake(scv).get().getFin();

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeInvalidClientFin() {
    byte[] clientHello = client.startHandshake();

    ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());
    byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get().getFin();

    // modify verification data
    clientFin[clientFin.length - 1]++;

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeInvalidServerFin() {
    byte[] clientHello = client.startHandshake();

    ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());

    ByteBuf bb = Unpooled.wrappedBuffer(shah.getServerHandshake());
    ServerHandshake handshake = ServerHandshake.parse(bb, true);

    byte[] vd = handshake.getServerHandshakeFinished().getVerificationData();

    vd[0]++; // modify verification data

    byte[] scv =
        Bytes.write(
            handshake.getEncryptedExtensions(),
            handshake.getServerCertificate(),
            handshake.getServerCertificateVerify(),
            new ServerHandshake.ServerHandshakeFinished(vd));

    client.handleHandshake(scv).get().getFin();
  }
}
