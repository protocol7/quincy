package com.protocol7.nettyquick;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.protocol7.nettyquick.client.QuicClient;
import com.protocol7.nettyquick.server.QuicServer;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;

public class Runner {

  public static void main(String[] args) throws Exception {
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9556);

    QuicServer server = QuicServer.bind(address, new StreamListener() {
      @Override
      public void onData(final Stream stream, final byte[] data) {
        System.out.println("s got " + new String(data) + " on "+ stream.getId());
        stream.write("Pong".getBytes(), true);
      }

      @Override
      public void onDone() {
        // do nothing
      }

      @Override
      public void onReset(final Stream stream, final int applicationErrorCode, final long offset) {
        // do nothing
      }
    }).sync().get();

    QuicClient client = QuicClient.connect(address, new StreamListener() {
      @Override
      public void onData(final Stream stream, final byte[] data) {
        System.out.println("c got " + new String(data));
      }

      @Override
      public void onDone() {
        // do nothing
      }

      @Override
      public void onReset(final Stream stream, final int applicationErrorCode, final long offset) {
        // do nothing
      }
    }).sync().get();

    Stream stream = client.openStream();
    stream.write("Ping".getBytes(), true);

    Thread.sleep(500);

    client.close().sync().await();
    server.close().sync().await();
  }
}
