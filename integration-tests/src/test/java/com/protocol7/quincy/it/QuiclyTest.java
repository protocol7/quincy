package com.protocol7.quincy.it;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.client.QuicClient;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.testcontainers.quicly.QuiclyPacket;
import com.protocol7.testcontainers.quicly.QuiclyServerContainer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;

public class QuiclyTest {

  @Rule public QuiclyServerContainer quicly = new QuiclyServerContainer();

  @Test
  public void quiclyServer() throws ExecutionException, InterruptedException {

    final BlockingQueue<byte[]> capturedData = new ArrayBlockingQueue<>(10);

    QuicClient client = null;
    try {
      client =
          QuicClient.connect(
                  Configuration.defaults(),
                  quicly.getAddress(),
                  new StreamListener() {
                    @Override
                    public void onData(final Stream stream, final byte[] data) {
                      capturedData.add(data);
                    }

                    @Override
                    public void onFinished() {}

                    @Override
                    public void onReset(
                        final Stream stream, final int applicationErrorCode, final long offset) {}
                  })
              .get();

      client.openStream().write("Hello world".getBytes(), true);

      final String actual =
          new String(capturedData.poll(10000, MILLISECONDS), StandardCharsets.US_ASCII);

      // checking that the error message is correct. Shows that we've been able to perform the
      // handshake and opening a stream
      assertTrue(actual.startsWith("HTTP/1.1 500 OK"));

      // TODO fix waiting for closing packets
      Thread.sleep(1000);

      final List<QuiclyPacket> packets = quicly.getPackets();

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
        try {
          client.close();
        } catch (final IllegalStateException ignored) {
        }
      }
    }
  }
}
