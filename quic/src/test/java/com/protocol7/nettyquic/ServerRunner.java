package com.protocol7.nettyquic;

import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.server.QuicServer;
import com.protocol7.nettyquic.streams.Stream;
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.nettyquic.tls.KeyUtil;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class ServerRunner {

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    QuicServer server =
        QuicServer.bind(
                Version.DRAFT_18,
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
                KeyUtil.getCertsFromCrt("quic/src/test/resources/server.crt"),
                KeyUtil.getPrivateKey("quic/src/test/resources/server.der"))
            .get();

    Thread.sleep(20000);

    server.close();
  }
}
