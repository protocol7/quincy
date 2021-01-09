package com.protocol7.quincy;

import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.tls.EncryptionLevel;

// TODO consolidate with Sender
public interface FrameSender {

  boolean isOpen();

  Packet sendPacket(Packet p);

  FullPacket send(EncryptionLevel level, Frame... frames);
}
