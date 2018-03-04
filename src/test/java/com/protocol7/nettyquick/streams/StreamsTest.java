package com.protocol7.nettyquick.streams;

import static org.junit.Assert.*;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.StreamId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StreamsTest {

  @Mock Connection connection;
  @Mock StreamListener listener;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void createAndThenGet() {
    Streams streams = new Streams(connection);
    StreamId streamId = StreamId.random(true, true);
    Stream stream1 = streams.getOrCreate(streamId, listener);
    Stream stream2 = streams.getOrCreate(streamId, listener);

    assertSame(stream1, stream2);

    Stream stream3 = streams.getOrCreate(StreamId.random(true, true), listener);
    assertNotSame(stream1, stream3);
  }
}