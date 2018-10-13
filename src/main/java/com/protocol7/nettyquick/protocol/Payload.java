package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.protocol.frames.Frame;

import java.util.List;

public interface Payload {
    List<Frame> getFrames();
}
