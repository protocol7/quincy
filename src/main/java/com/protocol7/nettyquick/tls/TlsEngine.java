package com.protocol7.nettyquick.tls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;

public class TlsEngine {

    private final Logger log = LoggerFactory.getLogger(TlsEngine.class);

    // TODO replace with config
    private static String keyStoreFile = "testkeys";
    private static String trustStoreFile = "testkeys";
    private static String passwd = "passphrase";

    private SSLEngine engine;

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

    private boolean clientMode;

    public TlsEngine(boolean clientMode) {
        this.clientMode = clientMode;

        reset();
    }

    public void reset() {
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

        return wrap();
    }

    public byte[] next(byte[] msg) {
        unwrap(msg);
        return wrap();
    }

    private byte[] wrap() {
        ByteBuffer in = allocate();
        ByteBuffer out = allocate();

        try {
            SSLEngineResult result = engine.wrap(in, out);

            log.debug("wrap: {}", resultToString(result));

            runDelegatedTasks(result);
            out.flip();
            byte[] b = new byte[out.limit()];
            out.get(b);
            return b;
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private void unwrap(byte[] msg) {
        ByteBuffer in = allocate();
        in.put(msg);
        in.flip();
        ByteBuffer out = allocate();

        try {
            SSLEngineResult result = engine.unwrap(in, out);

            log.debug("unwrap: {}", resultToString(result));

            runDelegatedTasks(result);

            if (out.position() > 0) {
                throw new IllegalStateException("TLS engine wrote on unwrap");
            }
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }

    }

    public void runDelegatedTasks(SSLEngineResult result) throws SSLException {

        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                log.debug("running delegated task...");
                runnable.run();
            }
            SSLEngineResult.HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw new SSLException(
                        "handshake shouldn't need additional tasks");
            }
            log.debug("new HandshakeStatus: {}", hsStatus);
        }
    }

    private String resultToString(SSLEngineResult result) {
        return result.toString().replace("\n", ", ");
    }

    private ByteBuffer allocate() {
        return ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
    }
}
