package com.protocol7.quincy.it;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.server.QuicServer;
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
                  new QuicBuilder().configuration(),
                  new InetSocketAddress("0.0.0.0", 4444),
                  (stream, data, finished) -> System.out.println(new String(data)),
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
