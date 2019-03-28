package com.protocol7.nettyquick.it;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.nettyquic.client.QuicClient;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.streams.Stream;
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.testcontainers.quicly.QuiclyContainer;
import com.protocol7.testcontainers.quicly.QuiclyPacket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;

public class QuiclyTest {

  @Rule public QuiclyContainer quicly = new QuiclyContainer();

  @Test
  public void test() throws ExecutionException, InterruptedException {

    final BlockingQueue<byte[]> capturedData = new ArrayBlockingQueue<>(10);

    QuicClient client = null;
    try {
      client =
          QuicClient.connect(
                  Version.DRAFT_18,
                  quicly.getAddress(),
                  new StreamListener() {
                    @Override
                    public void onData(Stream stream, byte[] data) {
                      capturedData.add(data);
                    }

                    @Override
                    public void onFinished() {}

                    @Override
                    public void onReset(Stream stream, int applicationErrorCode, long offset) {}
                  })
              .get();

      client.openStream().write("Hello world".getBytes(), true);

      String actual = new String(capturedData.poll(10000, MILLISECONDS), StandardCharsets.US_ASCII);

      // checking that the error message is correct. Shows that we've been able to perform the
      // handshake and opening a stream
      assertTrue(actual.startsWith("HTTP/1.1 500 OK"));

      // TODO fix waiting for closing packets
      Thread.sleep(1000);

      List<QuiclyPacket> packets = quicly.getPackets();

      // initial client hello
      assertTrue(packets.get(0).isInbound());

      // initial server hello
      assertFalse(packets.get(1).isInbound());

      // handshake
      assertFalse(packets.get(2).isInbound());

      // handshake ack
      assertTrue(packets.get(3).isInbound());
    } finally {
      if (client != null) {
        client.close();
      }
    }
  }
}
