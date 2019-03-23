package com.protocol7.nettyquic;

import com.protocol7.nettyquic.client.QuicClient;
import com.protocol7.nettyquic.streams.Stream;
import com.protocol7.nettyquic.streams.StreamListener;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class ClientRunner {

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    InetSocketAddress server = new InetSocketAddress("nghttp2.org", 4433);
    InetSocketAddress server2 = new InetSocketAddress("test.privateoctopus.com", 4433);
    InetSocketAddress server3 = new InetSocketAddress("127.0.0.1", 6121);

    QuicClient client =
        QuicClient.connect(
                server3,
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
