package com.protocol7.quincy;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.server.QuicServer;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.tls.KeyUtil;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class ServerRunner {

  public static void main(final String[] args) throws ExecutionException, InterruptedException {

    final QuicServer server =
        QuicServer.bind(
                new QuicBuilder().configuration(),
                new InetSocketAddress("0.0.0.0", 4444),
                new StreamListener() {
                  @Override
                  public void onData(
                      final Stream stream, final byte[] data, final boolean finished) {
                    System.out.println(new String(data));
                  }
                },
                KeyUtil.getCertsFromCrt("quic/src/test/resources/server.crt"),
                KeyUtil.getPrivateKey("quic/src/test/resources/server.der"))
            .get();

    Thread.sleep(20000);

    server.close();
  }
}
