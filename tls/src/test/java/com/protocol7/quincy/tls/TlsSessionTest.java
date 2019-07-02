package com.protocol7.quincy.tls;

import com.protocol7.quincy.tls.ClientTlsSession.CertificateInvalidException;
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

  private PrivateKey privateKey;
  private final ClientTlsSession client =
      new ClientTlsSession(
          InitialAEAD.create(Rnd.rndBytes(4), true),
          TestUtil.tps(),
          new NoopCertificateValidator());
  private ServerTlsSession server;

  @Before
  public void setUp() throws Exception {
    privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");
    final byte[] serverCert = KeyUtil.getCertFromCrt("src/test/resources/server.crt").getEncoded();

    server =
        new ServerTlsSession(
            InitialAEAD.create(Rnd.rndBytes(4), false),
            TestUtil.tps(),
            List.of(serverCert),
            privateKey);
  }

  @Test
  public void handshake() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake();

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());
    final byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get().getFin();

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeWithInvalidServerCertVerification() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake();

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());

    final ByteBuf bb = Unpooled.wrappedBuffer(shah.getServerHandshake());
    final ServerHandshake handshake = ServerHandshake.parse(bb, true);

    final byte[] sig = handshake.getServerCertificateVerify().getSignature();

    sig[0]++; // modify signature

    final byte[] scv =
        Bytes.write(
            handshake.getEncryptedExtensions(),
            handshake.getServerCertificate(),
            new ServerHandshake.ServerCertificateVerify(
                handshake.getServerCertificateVerify().getType(), sig),
            handshake.getServerHandshakeFinished());

    final byte[] clientFin = client.handleHandshake(scv).get().getFin();

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeInvalidClientFin() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake();

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());
    final byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get().getFin();

    // modify verification data
    clientFin[clientFin.length - 1]++;

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeInvalidServerFin() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake();

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());

    final ByteBuf bb = Unpooled.wrappedBuffer(shah.getServerHandshake());
    final ServerHandshake handshake = ServerHandshake.parse(bb, true);

    final byte[] vd = handshake.getServerHandshakeFinished().getVerificationData();

    vd[0]++; // modify verification data

    final byte[] scv =
        Bytes.write(
            handshake.getEncryptedExtensions(),
            handshake.getServerCertificate(),
            handshake.getServerCertificateVerify(),
            new ServerHandshake.ServerHandshakeFinished(vd));

    client.handleHandshake(scv).get().getFin();
  }
}
