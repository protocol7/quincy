package com.protocol7.nettyquick.protocol;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

import java.util.List;

public class ProtectedPayload implements Payload {

    public static ProtectedPayload parse(ByteBuf bb) {
        List<Frame> frames = Lists.newArrayList();
        while (bb.isReadable()) {
            frames.add(Frame.parse(bb));
        }
        return new ProtectedPayload(frames);
    }

    private List<Frame> frames;

    public ProtectedPayload(Frame... frames) {
        this(Lists.newArrayList(frames));
    }

    public ProtectedPayload(List<Frame> frames) {
        this.frames = frames;
    }

    public void write(ByteBuf bb) {
        for (Frame frame : frames) {
            frame.write(bb);
        }
    }

    public ProtectedPayload addFrame(Frame frame) {
        List<Frame> newFrames = Lists.newArrayList(frames);
        newFrames.add(frame);
        return new ProtectedPayload(newFrames);
    }

    @Override
    public List<Frame> getFrames() {
        return frames;
    }
}
