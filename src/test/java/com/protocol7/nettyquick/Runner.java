package com.protocol7.nettyquick;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.protocol7.nettyquick.client.QuicClient;
import com.protocol7.nettyquick.server.QuicServer;
import com.protocol7.nettyquick.streams.Stream;

public class Runner {

  public static void main(String[] args) throws Exception {
    InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 9556);

    QuicServer server = QuicServer.bind(address, (stream, offset, data) -> {
      System.out.println("s got " + new String(data));
      stream.write("Pong".getBytes(), true);
    }).sync().get();

    QuicClient client = QuicClient.connect(address, (stream, offset, data) -> System.out.println("c got " + new String(data))).sync().get();

    Stream stream = client.openStream();

    stream.write("Ping".getBytes(), true);

    Thread.sleep(500);

    client.close().sync().await();
    server.close().sync().await();
  }
}
