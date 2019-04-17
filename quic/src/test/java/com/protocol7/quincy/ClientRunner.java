package com.protocol7.quincy;

import com.protocol7.quincy.client.QuicClient;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class ClientRunner {

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    InetSocketAddress server1 = new InetSocketAddress("127.0.0.1", 6121);
    InetSocketAddress server2 = new InetSocketAddress("127.0.0.1", 4433);

    InetSocketAddress server = server2;

    QuicClient client =
        QuicClient.connect(
                Configuration.defaults(),
                server,
                new StreamListener() {
                  @Override
                  public void onData(Stream stream, byte[] data) {
                    System.out.println(new String(data));
                  }

                  @Override
                  public void onFinished() {}

                  @Override
                  public void onReset(Stream stream, int applicationErrorCode, long offset) {}
                })
            .get();

    client.openStream().write("Hello world".getBytes(), true);

    client.close();
  }
}
