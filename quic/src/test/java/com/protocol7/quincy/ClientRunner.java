package com.protocol7.quincy;

import com.protocol7.quincy.client.QuicClient;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class ClientRunner {

  public static void main(final String[] args) throws ExecutionException, InterruptedException {

    final InetSocketAddress server1 = new InetSocketAddress("127.0.0.1", 6121);
    final InetSocketAddress server2 = new InetSocketAddress("127.0.0.1", 4433);

    final InetSocketAddress server = server2;

    final QuicClient client =
        QuicClient.connect(
                Configuration.defaults(),
                server,
                new StreamListener() {
                  @Override
                  public void onData(final Stream stream, final byte[] data) {
                    System.out.println(new String(data));
                  }

                  @Override
                  public void onFinished() {}

                  @Override
                  public void onReset(
                      final Stream stream, final int applicationErrorCode, final long offset) {}
                })
            .get();

    client.openStream().write("Hello world".getBytes(), true);

    client.close();
  }
}
