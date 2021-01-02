package com.protocol7.quincy.tls;

import com.protocol7.quincy.tls.ClientTlsSession.CertificateInvalidException;
import com.protocol7.quincy.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.messages.EncryptedExtensions;
import com.protocol7.quincy.tls.messages.ServerCertificate;
import com.protocol7.quincy.tls.messages.ServerCertificateVerify;
import com.protocol7.quincy.tls.messages.ServerHandshakeFinished;
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
  private final byte[] connectionId = Rnd.rndBytes(16);
  private final ClientTlsSession client =
      new ClientTlsSession(
          InitialAEAD.create(connectionId, true),
          new byte[0],
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
    final byte[] clientHello = client.startHandshake(connectionId);

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());
    final byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get();

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeWithInvalidServerCertVerification() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake(connectionId);

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());

    final ByteBuf bb = Unpooled.wrappedBuffer(shah.getServerHandshake());
    final EncryptedExtensions parsedEE = EncryptedExtensions.parse(bb, true);
    final ServerCertificate parsedSC = ServerCertificate.parse(bb);
    final ServerCertificateVerify parsedSCV = ServerCertificateVerify.parse(bb);
    final ServerHandshakeFinished parsedSHE = ServerHandshakeFinished.parse(bb);

    final byte[] sig = parsedSCV.getSignature();

    sig[0]++; // modify signature

    final byte[] scv =
        Bytes.write(
            parsedEE, parsedSC, new ServerCertificateVerify(parsedSCV.getType(), sig), parsedSHE);

    final byte[] clientFin = client.handleHandshake(scv).get();

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeInvalidClientFin() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake(connectionId);

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());
    final byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get();

    // modify verification data
    clientFin[clientFin.length - 1]++;

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeInvalidServerFin() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake(connectionId);

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(shah.getServerHello());

    final ByteBuf bb = Unpooled.wrappedBuffer(shah.getServerHandshake());
    final EncryptedExtensions parsedEE = EncryptedExtensions.parse(bb, true);
    final ServerCertificate parsedSC = ServerCertificate.parse(bb);
    final ServerCertificateVerify parsedSCV = ServerCertificateVerify.parse(bb);
    final ServerHandshakeFinished parsedSHE = ServerHandshakeFinished.parse(bb);

    final byte[] vd = parsedSHE.getVerificationData();

    vd[0]++; // modify verification data

    final byte[] scv = Bytes.write(parsedEE, parsedSC, parsedSCV, new ServerHandshakeFinished(vd));

    client.handleHandshake(scv).get();
  }
}
