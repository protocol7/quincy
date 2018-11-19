package com.protocol7.nettyquick.tls;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

public class TlsSessions {

    @Test
    public void handshake() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        ClientTlsSession client = new ClientTlsSession();
        PrivateKey privateKey = KeyUtil.getPrivateKeyFromPem("src/test/resources/server.key");
        ServerTlsSession server = new ServerTlsSession(ImmutableList.of(), privateKey);

        byte[] clientHello = client.start();

        ServerTlsSession.ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

        client.handleServerHello(shah.getServerHello());
        byte[] clientFin = client.handleHandshake(shah.getServerHandshake()).get().getFin();

        server.handleClientFinished(clientFin);
    }
}
