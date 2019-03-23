package com.protocol7.nettyquic.streams;

import static org.junit.Assert.*;

import com.protocol7.nettyquic.connection.FrameSender;
import com.protocol7.nettyquic.protocol.StreamId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StreamsTest {

  @Mock FrameSender sender;
  @Mock StreamListener listener;

  private Streams streams;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.streams = new Streams(sender);
  }

  @Test
  public void openStream() {
    Stream stream = streams.openStream(false, false, listener);
    assertEquals(new StreamId(3), stream.getId());
  }

  @Test
  public void createAndThenGet() {
    StreamId streamId = StreamId.random(true, true);
    DefaultStream stream1 = streams.getOrCreate(streamId, listener);
    DefaultStream stream2 = streams.getOrCreate(streamId, listener);

    assertSame(stream1, stream2);

    DefaultStream stream3 = streams.getOrCreate(StreamId.random(true, true), listener);
    assertNotSame(stream1, stream3);
  }
}
