package com.protocol7.nettyquick;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.client.ClientConnection;
import com.protocol7.nettyquick.client.ClientHandler;
import com.protocol7.nettyquick.client.QuicClient;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.CryptoFrame;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.PaddingFrame;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.tls.TlsEngine;
import com.protocol7.nettyquick.utils.Futures;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class ClientRunner {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        InetSocketAddress server = new InetSocketAddress("nghttp2.org", 4433);
        InetSocketAddress server2 = new InetSocketAddress("test.privateoctopus.com", 4433);
        InetSocketAddress server3 = new InetSocketAddress("127.0.0.1", 6121);

        TlsEngine tlsEngine = new TlsEngine(true);

        NioEventLoopGroup group = new NioEventLoopGroup();
        ClientHandler handler = new ClientHandler();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .handler(handler);

        Future<Channel> channelFuture = Futures.thenChannel(GlobalEventExecutor.INSTANCE, b.bind(0));


        ConnectionId connectionId = ConnectionId.random();

        ClientConnection conn = new ClientConnection(connectionId, group, channelFuture.get(), server3, new StreamListener() {
            @Override
            public void onData(Stream stream, byte[] data) {
                System.out.println(new String(data));
            }

            @Override
            public void onDone() {

            }

            @Override
            public void onReset(Stream stream, int applicationErrorCode, long offset) {

            }
        });
        handler.setConnection(conn);

        conn.handshake();
    }
}
