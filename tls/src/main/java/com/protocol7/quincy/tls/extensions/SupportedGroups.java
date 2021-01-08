package com.protocol7.quincy.tls.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.protocol7.quincy.tls.Group;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SupportedGroups implements Extension {

  public static SupportedGroups parse(final ByteBuf bb) {
    bb.readShort();

    final Builder<Group> groups = ImmutableList.builder();

    while (bb.isReadable()) {
      final Optional<Group> group = Group.fromValue(bb.readShort());
      group.ifPresent(groups::add);
    }

    return new SupportedGroups(groups.build());
  }

  private final List<Group> groups;

  public SupportedGroups(final List<Group> groups) {
    this.groups = groups;
  }

  public SupportedGroups(final Group... groups) {
    this.groups = ImmutableList.copyOf(groups);
  }

  public List<Group> getGroups() {
    return groups;
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.SUPPORTED_GROUPS;
  }

  @Override
  public void write(final ByteBuf bb, final boolean isClient) {
    bb.writeShort(groups.size() * 2);

    for (final Group group : groups) {
      bb.writeShort(group.getValue());
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final SupportedGroups that = (SupportedGroups) o;
    return Objects.equals(groups, that.groups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groups);
  }

  @Override
  public String toString() {
    return "SupportedGroups{" + groups + '}';
  }
}
