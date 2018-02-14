package com.protocol7.nettyquick;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.protocol7.nettyquick.client.ClientStream;
import com.protocol7.nettyquick.client.QuicClient;
import com.protocol7.nettyquick.server.QuicServer;

public class Runner {

  public static void main(String[] args) throws Exception {
    InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 9556);
    QuicServer server = QuicServer.bind(address, (stream, data) -> {
      System.out.println("s got " + new String(data));
      stream.write("Pong".getBytes());
    });

    QuicClient client = QuicClient.connect(address, data -> System.out.println("c got " + new String(data)));

    ClientStream stream = client.openStream();

    stream.write("Ping".getBytes());

    Thread.sleep(100);

    client.close(); //.sync().await();
    server.close(); //.sync().await();
  }
}
