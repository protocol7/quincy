package com.protocol7.nettyquick.tls.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.protocol7.nettyquick.tls.Group;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;

public class SupportedGroups implements Extension {

  public static SupportedGroups parse(ByteBuf bb) {
    bb.readShort();

    Builder<Group> groups = ImmutableList.builder();

    while (bb.isReadable()) {
      Optional<Group> group = Group.fromValue(bb.readShort());
      group.ifPresent(groups::add);
    }

    return new SupportedGroups(groups.build());
  }

  private final List<Group> groups;

  public SupportedGroups(List<Group> groups) {
    this.groups = groups;
  }

  public SupportedGroups(Group... groups) {
    this.groups = ImmutableList.copyOf(groups);
  }

  public List<Group> getGroups() {
    return groups;
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.supported_groups;
  }

  @Override
  public void write(ByteBuf bb, boolean isClient) {
    bb.writeShort(groups.size() * 2);

    for (Group group : groups) {
      bb.writeShort(group.getValue());
    }
  }

  @Override
  public String toString() {
    return "SupportedGroups{" + groups + '}';
  }
}
