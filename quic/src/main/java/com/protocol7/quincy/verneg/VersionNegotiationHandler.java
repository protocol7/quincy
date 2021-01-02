package com.protocol7.quincy.verneg;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class VersionNegotiationHandler extends ChannelInboundHandlerAdapter {

  private static final int LONG_PACKET_TYPE = 0b10000000;

  private final Version supportedVersion;

  public VersionNegotiationHandler(final Version supportedVersion) {
    this.supportedVersion = supportedVersion;
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {

    if (msg instanceof ByteBuf) {
      final ByteBuf bb = (ByteBuf) msg;
      bb.markReaderIndex();

      final byte firstByte = bb.readByte();

      if ((firstByte & LONG_PACKET_TYPE) == LONG_PACKET_TYPE) {
        // long packet
        final Version version = Version.read(bb);

        if (version == supportedVersion) {
          // good to go
          bb.resetReaderIndex();
          ctx.fireChannelRead(bb);
        } else {
          // send back verneg packet
          final ConnectionId dcid = ConnectionId.read(bb);
          final ConnectionId scid = ConnectionId.read(bb);
          final VersionNegotiationPacket response =
              new VersionNegotiationPacket(scid, dcid, supportedVersion);

          final ByteBuf out = Unpooled.buffer();
          response.write(out, null);

          ctx.writeAndFlush(out);
        }
      } else {
        // short packet, good to go
        bb.resetReaderIndex();
        ctx.fireChannelRead(bb);
      }
    } else {
      throw new IllegalArgumentException("Expected ByteBuf message");
    }
  }
}
