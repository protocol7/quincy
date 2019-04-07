package com.protocol7.nettyquick.it;

import com.protocol7.nettyquic.Configuration;
import com.protocol7.nettyquic.server.QuicServer;
import com.protocol7.nettyquic.streams.Stream;
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.testcontainers.quicly.QuiclyClientContainer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

public class QuiclyClientTest {

  @Test
  public void quiclyClient() throws ExecutionException, InterruptedException, IOException {

    QuicServer server = null;
    try {
      server =
          QuicServer.bind(
                  Configuration.defaults(),
                  new InetSocketAddress("0.0.0.0", 4444),
                  new StreamListener() {
                    @Override
                    public void onData(Stream stream, byte[] data) {
                      System.out.println(new String(data));
                    }

                    @Override
                    public void onFinished() {}

                    @Override
                    public void onReset(Stream stream, int applicationErrorCode, long offset) {}
                  },
                  KeyUtil.getCertsFromCrt("src/test/resources/server.crt"),
                  KeyUtil.getPrivateKey("src/test/resources/server.der"))
              .get();

      Thread.sleep(1000);

      QuiclyClientContainer quicly = null;
      try {
        quicly = new QuiclyClientContainer("host.docker.internal", 4444);
        quicly.withCommand(
            "./cli", "-vv", "-x", "X25519", "host.docker.internal", Integer.toString(4444));
        quicly.start();

        // TODO replace with verification
        Thread.sleep(3000);
      } finally {
        if (quicly != null) {
          quicly.stop();
          quicly.close();
        }
      }
    } finally {
      if (server != null) {
        server.close();
      }
    }
  }
}
