package com.protocol7.quincy;

import com.protocol7.quincy.client.QuicClient;
import com.protocol7.quincy.netty.QuicBuilder;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class ClientRunner {

  public static void main(final String[] args) throws ExecutionException, InterruptedException {

    final InetSocketAddress server1 = new InetSocketAddress("127.0.0.1", 6121);
    final InetSocketAddress server2 = new InetSocketAddress("127.0.0.1", 4433);

    final InetSocketAddress server = server2;

    final QuicClient client =
        QuicClient.connect(
                new QuicBuilder().configuration(),
                server,
                (stream, data, finished) -> System.out.println(new String(data)))
            .get();

    client.openStream().write("Hello world".getBytes(), true);

    client.close();
  }
}
