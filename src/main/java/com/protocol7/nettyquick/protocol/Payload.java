package com.protocol7.nettyquick.protocol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Payload {

  public static Payload parse(ByteBuf bb, int length, AEAD aead, PacketNumber pn, byte[] aad) {
    byte[] cipherText = new byte[length];
    bb.readBytes(cipherText);

    byte[]raw;
    try {
      raw = aead.open(cipherText, pn.asLong(), aad);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }

    List<Frame> frames = Lists.newArrayList();
    ByteBuf frameBuf = Unpooled.wrappedBuffer(raw);

    while (frameBuf.isReadable()) {
      Frame frame = Frame.parse(frameBuf);
      // TODO ignore padding frames?
      frames.add(frame);
    }
    return new Payload(frames);
  }

  public Payload addFrame(Frame frame) {
    List<Frame> newFrames = Lists.newArrayList(frames);
    newFrames.add(frame);
    return new Payload(newFrames);
  }

  private final List<Frame> frames;

  public Payload(final List<Frame> frames) {
    checkNotNull(frames);
    checkArgument(!frames.isEmpty());

    this.frames = ImmutableList.copyOf(frames);
  }

  public Payload(final Frame... frames) {
    this(Arrays.asList(frames));
  }

  public List<Frame> getFrames() {
    return frames;
  }

  public int getLength() {
    return frames.stream().mapToInt(f -> f.calculateLength()).sum() + AEAD.OVERHEAD; // AEAD overhead
  }

  public void write(ByteBuf bb, AEAD aead, PacketNumber pn, byte[] aad) {
    ByteBuf raw = Unpooled.buffer();
    for (Frame frame : frames) {
        frame.write(raw);
    }
    byte[] b = Bytes.drainToArray(raw);

    try {
      byte[] sealed = aead.seal(b, pn.asLong(), aad);
      bb.writeBytes(sealed);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final Payload payload = (Payload) o;

    return frames != null ? frames.equals(payload.frames) : payload.frames == null;

  }

  @Override
  public int hashCode() {
    return frames != null ? frames.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Payload{" + frames + '}';
  }
}
