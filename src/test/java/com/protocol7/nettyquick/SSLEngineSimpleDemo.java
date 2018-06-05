package com.protocol7.nettyquick;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

public class SSLEngineSimpleDemo {

  /*
   * The following is to set up the keystores.
   */
  private static String keyStoreFile = "testkeys";
  private static String trustStoreFile = "testkeys";

  /*
   * Main entry point for this demo.
   */
  public static void main(String args[]) throws Exception {
    //System.setProperty("javax.net.debug", "all");

    KeyStore ks = KeyStore.getInstance("JKS");
    KeyStore ts = KeyStore.getInstance("JKS");

    char[] passphrase = "passphrase".toCharArray();

    ks.load(new FileInputStream(keyStoreFile), passphrase);
    ts.load(new FileInputStream(trustStoreFile), passphrase);

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(ks, passphrase);

    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(ts);

    SSLContext sslCtx = SSLContext.getInstance("TLS");

    sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    SSLContext sslc = sslCtx;

    boolean dataDone = false;

        /*
         * Configure the serverEngine to act as a server in the SSL/TLS
         * handshake.  Also, require SSL client authentication.
         */
    SSLEngine serverEngine = sslc.createSSLEngine();
    serverEngine.setUseClientMode(false);
    serverEngine.setNeedClientAuth(true);

        /*
         * Similar to above, but using client mode instead.
         */
    SSLEngine clientEngine = sslc.createSSLEngine("client", 80);
    clientEngine.setUseClientMode(true);


        /*
         * We'll assume the buffer sizes are the same
         * between client and server.
         */
    SSLSession session = clientEngine.getSession();
    int appBufferMax = session.getApplicationBufferSize();
    int netBufferMax = session.getPacketBufferSize();

        /*
         * We'll make the input buffers a bit bigger than the max needed
         * size, so that unwrap()s following a successful data transfer
         * won't generate BUFFER_OVERFLOWS.
         *
         * We'll use a mix of direct and indirect ByteBuffers for
         * tutorial purposes only.  In reality, only use direct
         * ByteBuffers when they give a clear performance enhancement.
         */
    ByteBuffer clientIn = ByteBuffer.allocate(appBufferMax + 50);
    ByteBuffer serverIn = ByteBuffer.allocate(appBufferMax + 50);

    ByteBuffer clientOut = ByteBuffer.wrap("Hi Server, I'm Client".getBytes());
    ByteBuffer serverOut = ByteBuffer.wrap("Hello Client, I'm Server".getBytes());

        /*
         * Examining the SSLEngineResults could be much more involved,
         * and may alter the overall flow of the application.
         *
         * For example, if we received a BUFFER_OVERFLOW when trying
         * to write to the output pipe, we could reallocate a larger
         * pipe, but instead we wait for the peer to drain it.
         */

    serverEngine.beginHandshake();
    clientEngine.beginHandshake();



    while (!isEngineClosed(clientEngine) ||
            !isEngineClosed(serverEngine)) {

      loopUntil(serverEngine, clientEngine, clientIn, serverIn, clientOut, serverOut, netBufferMax, HandshakeStatus.NOT_HANDSHAKING);

            /*
             * After we've transfered all application data between the client
             * and server, we close the clientEngine's outbound stream.
             * This generates a close_notify handshake message, which the
             * server engine receives and responds by closing itself.
             *
             * In normal operation, each SSLEngine should call
             * closeOutbound().  To protect against truncation attacks,
             * SSLEngine.closeInbound() should be called whenever it has
             * determined that no more input data will ever be
             * available (say a closed input stream).
             */
      if (!dataDone && (clientOut.limit() == serverIn.position()) &&
              (serverOut.limit() == clientIn.position())) {

                /*
                 * A sanity check to ensure we got what was sent.
                 */
        checkTransfer(serverOut, clientIn);
        checkTransfer(clientOut, serverIn);

        log("\tClosing clientEngine's *OUTBOUND*...");
        clientEngine.closeOutbound();
        // serverEngine.closeOutbound();
        dataDone = true;
      }
    }
  }

  private static void loopUntil(final SSLEngine serverEngine, final SSLEngine clientEngine,
                                final ByteBuffer clientIn, final ByteBuffer serverIn,
                                final ByteBuffer clientOut, final ByteBuffer serverOut,
                                final int netBufferMax, final HandshakeStatus until) throws Exception {
    ByteBuffer cTOs = ByteBuffer.allocate(netBufferMax);
    ByteBuffer sTOc = ByteBuffer.allocate(netBufferMax);

    //while (clientEngine.getHandshakeStatus() != until || serverEngine.getHandshakeStatus() != until) {
      log("================");

      wrap(clientEngine, clientOut, cTOs, "client wrap: ");
      wrap(serverEngine, serverOut, sTOc, "server wrap: ");

      cTOs.flip();
      sTOc.flip();

      log("----");

      unwrap(clientEngine, sTOc, clientIn, "client unwrap: ");

      unwrap(serverEngine, cTOs, serverIn, "server unwrap: ");

      cTOs.compact();
      sTOc.compact();
    //}
  }

  private static void unwrap(final SSLEngine engine, final ByteBuffer in, final ByteBuffer out, final String msg) throws Exception {
    final SSLEngineResult result = engine.unwrap(in, out);
    log(msg, result, engine);
    runDelegatedTasks(result, engine);
  }

  private static void wrap(final SSLEngine engine, final ByteBuffer in, final ByteBuffer out, final String msg) throws Exception {
    SSLEngineResult result = engine.wrap(in, out);
    log(msg, result, engine);
    runDelegatedTasks(result, engine);
  }

  /*
   * If the result indicates that we have outstanding tasks to do,
   * go ahead and run them in this thread.
   */
  private static void runDelegatedTasks(SSLEngineResult result, SSLEngine engine) throws Exception {
    if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
      Runnable runnable;
      while ((runnable = engine.getDelegatedTask()) != null) {
        log("\trunning delegated task...");
        runnable.run();
      }
      HandshakeStatus hsStatus = engine.getHandshakeStatus();
      if (hsStatus == HandshakeStatus.NEED_TASK) {
        throw new Exception(
                "handshake shouldn't need additional tasks");
      }
      log("\tnew HandshakeStatus: " + hsStatus);
    }
  }

  private static boolean isEngineClosed(SSLEngine engine) {
    return (engine.isOutboundDone() && engine.isInboundDone());
  }

  /*
   * Simple check to make sure everything came across as expected.
   */
  private static void checkTransfer(ByteBuffer a, ByteBuffer b)
          throws Exception {
    a.flip();
    b.flip();

    if (!a.equals(b)) {
      throw new Exception("Data didn't transfer cleanly");
    } else {
      log("\tData transferred cleanly");
    }

    a.position(a.limit());
    b.position(b.limit());
    a.limit(a.capacity());
    b.limit(b.capacity());
  }

  private static void log(String str, SSLEngineResult result, SSLEngine engine) {
    HandshakeStatus hsStatus = result.getHandshakeStatus();
    log(str +
                engine.getHandshakeStatus() + "/" + result.getStatus() + "/" + hsStatus + ", " +
                result.bytesConsumed() + "/" + result.bytesProduced() +
                " bytes");
    if (hsStatus == HandshakeStatus.FINISHED) {
      log("\t...ready for application data");
    }
  }

  private static void log(String str) {
    System.out.println(str);
  }
}