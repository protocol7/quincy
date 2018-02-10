package com.protocol7.nettyquick;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.protocol7.nettyquick.client.QuicClient;
import com.protocol7.nettyquick.client.Stream;
import com.protocol7.nettyquick.server.QuicServer;

public class Runner {

  public static void main(String[] args) throws Exception {
    InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 9556);
    QuicServer server = QuicServer.bind(address);

    QuicClient client = QuicClient.connect(address);

    Stream stream = client.openStream(data -> System.out.println(new String(data)));

    stream.write("Ping".getBytes()).sync().await();

    Thread.sleep(100);

    client.close(); //.sync().await();
    System.out.println("Client closed");
    server.close(); //.sync().await();
    System.out.println("Server closed");
  }
}
