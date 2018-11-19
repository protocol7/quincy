package com.protocol7.nettyquick.tls;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.nettyquick.utils.Hex;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;

public class TlsSessions {

    private PrivateKey privateKey;
    private final ClientTlsSession client = new ClientTlsSession();
    private ServerTlsSession server;

    @Before
    public void setUp() throws Exception {
        privateKey = KeyUtil.getPrivateKeyFromPem("src/test/resources/server.key");

        byte[] serverCert = KeyUtil.getCertFromCrt("src/test/resources/server.crt").getEncoded();

        server = new ServerTlsSession(ImmutableList.of(serverCert), privateKey);
    }

    @Test
    public void handshake() {
        byte[] clientHello = client.start();

        ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

        client.handleServerHello(shah.getServerHello());
        byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get().getFin();

        server.handleClientFinished(clientFin);
    }

    @Test(expected = RuntimeException.class)
    public void handshakeInvalidClientFin() {
        byte[] clientHello = client.start();

        ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

        client.handleServerHello(shah.getServerHello());
        byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get().getFin();

        // modify verification data
        clientFin[clientFin.length - 1]++;

        server.handleClientFinished(clientFin);
    }
}
