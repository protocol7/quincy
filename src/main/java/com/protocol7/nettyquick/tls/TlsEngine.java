package com.protocol7.nettyquick.tls;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;

public class TlsEngine {

    // TODO replace with config
    private static String keyStoreFile = "testkeys";
    private static String trustStoreFile = "testkeys";
    private static String passwd = "passphrase";

    private final SSLEngine engine;

    private static KeyManagerFactory createKeyManagerFactory(String passphrase) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] pp = passphrase.toCharArray();
        ks.load(new FileInputStream(keyStoreFile), pp);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, pp);
        return kmf;
    }

    private static TrustManagerFactory createTrustManagerFactory(String passphrase) throws Exception {
        KeyStore ts = KeyStore.getInstance("JKS");
        char[] pp = passphrase.toCharArray();
        ts.load(new FileInputStream(trustStoreFile), pp);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);
        return tmf;
    }

    private final boolean clientMode;

    public TlsEngine(boolean clientMode) {
        try {
            SSLContext sslCtx = SSLContext.getInstance("TLS");

            sslCtx.init(
                    createKeyManagerFactory(passwd).getKeyManagers(),
                    createTrustManagerFactory(passwd).getTrustManagers(),
                    null);

            this.engine = sslCtx.createSSLEngine();
            engine.setUseClientMode(clientMode);
            engine.setEnabledProtocols(new String[]{"TLSv1.3"});
            engine.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"}); // TODO make configurable

            this.clientMode = clientMode;
        } catch (Exception e) {
            throw new RuntimeException("Failed to init TLS", e);
        }
    }

    public byte[] start() {
        if (!clientMode) {
            throw new IllegalStateException("Can not start in server mode");
        }

        ByteBuffer in = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        ByteBuffer out = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());

        try {
            engine.wrap(in, out);
            out.flip();
            byte[] b = new byte[out.limit()];
            out.get(b);
            return b;
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] next(byte[] msg) {
        return new byte[]{'S', 'H'};
    }
}
