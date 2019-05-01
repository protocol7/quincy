/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http2;

import static io.netty.handler.codec.CharSequenceValueConverter.*;
import static io.netty.handler.codec.http2.DefaultHttp2Headers.*;
import static io.netty.util.AsciiString.*;
import static io.netty.util.internal.EmptyArrays.*;

import io.netty.handler.codec.Headers;
import io.netty.util.AsciiString;
import io.netty.util.HashingStrategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A variant of {@link Http2Headers} which only supports read-only methods.
 *
 * <p>Any array passed to this class may be used directly in the underlying data structures of this
 * class. If these arrays may be modified it is the caller's responsibility to supply this class
 * with a copy of the array.
 *
 * <p>This may be a good alternative to {@link DefaultHttp2Headers} if your have a fixed set of
 * headers which will not change.
 */
public final class ReadOnlyHttp2Headers implements Http2Headers {
  private static final byte PSEUDO_HEADER_TOKEN = (byte) ':';
  private final AsciiString[] pseudoHeaders;
  private final AsciiString[] otherHeaders;

  /**
   * Used to create read only object designed to represent trailers.
   *
   * <p>If this is used for a purpose other than trailers you may violate the header serialization
   * ordering defined by <a href="https://tools.ietf.org/html/rfc7540#section-8.1.2.1">RFC 7540,
   * 8.1.2.1</a>.
   *
   * @param validateHeaders {@code true} will run validation on each header name/value pair to
   *     ensure protocol compliance.
   * @param otherHeaders A an array of key:value pairs. Must not contain any <a
   *     href="https://tools.ietf.org/html/rfc7540#section-8.1.2.1">pseudo headers</a> or {@code
   *     null} names/values. A copy will <strong>NOT</strong> be made of this array. If the contents
   *     of this array may be modified externally you are responsible for passing in a copy.
   * @return A read only representation of the headers.
   */
  public static ReadOnlyHttp2Headers trailers(
      final boolean validateHeaders, final AsciiString... otherHeaders) {
    return new ReadOnlyHttp2Headers(validateHeaders, EMPTY_ASCII_STRINGS, otherHeaders);
  }

  /**
   * Create a new read only representation of headers used by clients.
   *
   * @param validateHeaders {@code true} will run validation on each header name/value pair to
   *     ensure protocol compliance.
   * @param method The value for {@link PseudoHeaderName#METHOD}.
   * @param path The value for {@link PseudoHeaderName#PATH}.
   * @param scheme The value for {@link PseudoHeaderName#SCHEME}.
   * @param authority The value for {@link PseudoHeaderName#AUTHORITY}.
   * @param otherHeaders A an array of key:value pairs. Must not contain any <a
   *     href="https://tools.ietf.org/html/rfc7540#section-8.1.2.1">pseudo headers</a> or {@code
   *     null} names/values. A copy will <strong>NOT</strong> be made of this array. If the contents
   *     of this array may be modified externally you are responsible for passing in a copy.
   * @return a new read only representation of headers used by clients.
   */
  public static ReadOnlyHttp2Headers clientHeaders(
      final boolean validateHeaders,
      final AsciiString method,
      final AsciiString path,
      final AsciiString scheme,
      final AsciiString authority,
      final AsciiString... otherHeaders) {
    return new ReadOnlyHttp2Headers(
        validateHeaders,
        new AsciiString[] {
          PseudoHeaderName.METHOD.value(), method, PseudoHeaderName.PATH.value(), path,
          PseudoHeaderName.SCHEME.value(), scheme, PseudoHeaderName.AUTHORITY.value(), authority
        },
        otherHeaders);
  }

  /**
   * Create a new read only representation of headers used by servers.
   *
   * @param validateHeaders {@code true} will run validation on each header name/value pair to
   *     ensure protocol compliance.
   * @param status The value for {@link PseudoHeaderName#STATUS}.
   * @param otherHeaders A an array of key:value pairs. Must not contain any <a
   *     href="https://tools.ietf.org/html/rfc7540#section-8.1.2.1">pseudo headers</a> or {@code
   *     null} names/values. A copy will <strong>NOT</strong> be made of this array. If the contents
   *     of this array may be modified externally you are responsible for passing in a copy.
   * @return a new read only representation of headers used by servers.
   */
  public static ReadOnlyHttp2Headers serverHeaders(
      final boolean validateHeaders, final AsciiString status, final AsciiString... otherHeaders) {
    return new ReadOnlyHttp2Headers(
        validateHeaders, new AsciiString[] {PseudoHeaderName.STATUS.value(), status}, otherHeaders);
  }

  private ReadOnlyHttp2Headers(
      final boolean validateHeaders,
      final AsciiString[] pseudoHeaders,
      final AsciiString... otherHeaders) {
    assert (pseudoHeaders.length & 1)
        == 0; // pseudoHeaders are only set internally so assert should be enough.
    if ((otherHeaders.length & 1) != 0) {
      throw newInvalidArraySizeException();
    }
    if (validateHeaders) {
      validateHeaders(pseudoHeaders, otherHeaders);
    }
    this.pseudoHeaders = pseudoHeaders;
    this.otherHeaders = otherHeaders;
  }

  private static IllegalArgumentException newInvalidArraySizeException() {
    return new IllegalArgumentException(
        "pseudoHeaders and otherHeaders must be arrays of [name, value] pairs");
  }

  private static void validateHeaders(
      final AsciiString[] pseudoHeaders, final AsciiString... otherHeaders) {
    // We are only validating values... so start at 1 and go until end.
    for (int i = 1; i < pseudoHeaders.length; i += 2) {
      // pseudoHeaders names are only set internally so they are assumed to be valid.
      if (pseudoHeaders[i] == null) {
        throw new IllegalArgumentException("pseudoHeaders value at index " + i + " is null");
      }
    }

    boolean seenNonPseudoHeader = false;
    final int otherHeadersEnd = otherHeaders.length - 1;
    for (int i = 0; i < otherHeadersEnd; i += 2) {
      final AsciiString name = otherHeaders[i];
      HTTP2_NAME_VALIDATOR.validateName(name);
      if (!seenNonPseudoHeader && !name.isEmpty() && name.byteAt(0) != PSEUDO_HEADER_TOKEN) {
        seenNonPseudoHeader = true;
      } else if (seenNonPseudoHeader && !name.isEmpty() && name.byteAt(0) == PSEUDO_HEADER_TOKEN) {
        throw new IllegalArgumentException(
            "otherHeaders name at index "
                + i
                + " is a pseudo header that appears after non-pseudo headers.");
      }
      if (otherHeaders[i + 1] == null) {
        throw new IllegalArgumentException("otherHeaders value at index " + (i + 1) + " is null");
      }
    }
  }

  private AsciiString get0(final CharSequence name) {
    final int nameHash = AsciiString.hashCode(name);

    final int pseudoHeadersEnd = pseudoHeaders.length - 1;
    for (int i = 0; i < pseudoHeadersEnd; i += 2) {
      final AsciiString roName = pseudoHeaders[i];
      if (roName.hashCode() == nameHash && roName.contentEqualsIgnoreCase(name)) {
        return pseudoHeaders[i + 1];
      }
    }

    final int otherHeadersEnd = otherHeaders.length - 1;
    for (int i = 0; i < otherHeadersEnd; i += 2) {
      final AsciiString roName = otherHeaders[i];
      if (roName.hashCode() == nameHash && roName.contentEqualsIgnoreCase(name)) {
        return otherHeaders[i + 1];
      }
    }
    return null;
  }

  @Override
  public CharSequence get(final CharSequence name) {
    return get0(name);
  }

  @Override
  public CharSequence get(final CharSequence name, final CharSequence defaultValue) {
    final CharSequence value = get(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public CharSequence getAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public CharSequence getAndRemove(final CharSequence name, final CharSequence defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public List<CharSequence> getAll(final CharSequence name) {
    final int nameHash = AsciiString.hashCode(name);
    final List<CharSequence> values = new ArrayList<CharSequence>();

    final int pseudoHeadersEnd = pseudoHeaders.length - 1;
    for (int i = 0; i < pseudoHeadersEnd; i += 2) {
      final AsciiString roName = pseudoHeaders[i];
      if (roName.hashCode() == nameHash && roName.contentEqualsIgnoreCase(name)) {
        values.add(pseudoHeaders[i + 1]);
      }
    }

    final int otherHeadersEnd = otherHeaders.length - 1;
    for (int i = 0; i < otherHeadersEnd; i += 2) {
      final AsciiString roName = otherHeaders[i];
      if (roName.hashCode() == nameHash && roName.contentEqualsIgnoreCase(name)) {
        values.add(otherHeaders[i + 1]);
      }
    }

    return values;
  }

  @Override
  public List<CharSequence> getAllAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Boolean getBoolean(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToBoolean(value) : null;
  }

  @Override
  public boolean getBoolean(final CharSequence name, final boolean defaultValue) {
    final Boolean value = getBoolean(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Byte getByte(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToByte(value) : null;
  }

  @Override
  public byte getByte(final CharSequence name, final byte defaultValue) {
    final Byte value = getByte(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Character getChar(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToChar(value) : null;
  }

  @Override
  public char getChar(final CharSequence name, final char defaultValue) {
    final Character value = getChar(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Short getShort(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToShort(value) : null;
  }

  @Override
  public short getShort(final CharSequence name, final short defaultValue) {
    final Short value = getShort(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Integer getInt(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToInt(value) : null;
  }

  @Override
  public int getInt(final CharSequence name, final int defaultValue) {
    final Integer value = getInt(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Long getLong(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToLong(value) : null;
  }

  @Override
  public long getLong(final CharSequence name, final long defaultValue) {
    final Long value = getLong(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Float getFloat(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToFloat(value) : null;
  }

  @Override
  public float getFloat(final CharSequence name, final float defaultValue) {
    final Float value = getFloat(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Double getDouble(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToDouble(value) : null;
  }

  @Override
  public double getDouble(final CharSequence name, final double defaultValue) {
    final Double value = getDouble(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Long getTimeMillis(final CharSequence name) {
    final AsciiString value = get0(name);
    return value != null ? INSTANCE.convertToTimeMillis(value) : null;
  }

  @Override
  public long getTimeMillis(final CharSequence name, final long defaultValue) {
    final Long value = getTimeMillis(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Boolean getBooleanAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public boolean getBooleanAndRemove(final CharSequence name, final boolean defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Byte getByteAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public byte getByteAndRemove(final CharSequence name, final byte defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Character getCharAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public char getCharAndRemove(final CharSequence name, final char defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Short getShortAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public short getShortAndRemove(final CharSequence name, final short defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Integer getIntAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public int getIntAndRemove(final CharSequence name, final int defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Long getLongAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public long getLongAndRemove(final CharSequence name, final long defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Float getFloatAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public float getFloatAndRemove(final CharSequence name, final float defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Double getDoubleAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public double getDoubleAndRemove(final CharSequence name, final double defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Long getTimeMillisAndRemove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public long getTimeMillisAndRemove(final CharSequence name, final long defaultValue) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public boolean contains(final CharSequence name) {
    return get(name) != null;
  }

  @Override
  public boolean contains(final CharSequence name, final CharSequence value) {
    return contains(name, value, false);
  }

  @Override
  public boolean containsObject(final CharSequence name, final Object value) {
    if (value instanceof CharSequence) {
      return contains(name, (CharSequence) value);
    }
    return contains(name, value.toString());
  }

  @Override
  public boolean containsBoolean(final CharSequence name, final boolean value) {
    return contains(name, String.valueOf(value));
  }

  @Override
  public boolean containsByte(final CharSequence name, final byte value) {
    return contains(name, String.valueOf(value));
  }

  @Override
  public boolean containsChar(final CharSequence name, final char value) {
    return contains(name, String.valueOf(value));
  }

  @Override
  public boolean containsShort(final CharSequence name, final short value) {
    return contains(name, String.valueOf(value));
  }

  @Override
  public boolean containsInt(final CharSequence name, final int value) {
    return contains(name, String.valueOf(value));
  }

  @Override
  public boolean containsLong(final CharSequence name, final long value) {
    return contains(name, String.valueOf(value));
  }

  @Override
  public boolean containsFloat(final CharSequence name, final float value) {
    return false;
  }

  @Override
  public boolean containsDouble(final CharSequence name, final double value) {
    return contains(name, String.valueOf(value));
  }

  @Override
  public boolean containsTimeMillis(final CharSequence name, final long value) {
    return contains(name, String.valueOf(value));
  }

  @Override
  public int size() {
    return (pseudoHeaders.length + otherHeaders.length) >>> 1;
  }

  @Override
  public boolean isEmpty() {
    return pseudoHeaders.length == 0 && otherHeaders.length == 0;
  }

  @Override
  public Set<CharSequence> names() {
    if (isEmpty()) {
      return Collections.emptySet();
    }
    final Set<CharSequence> names = new LinkedHashSet<CharSequence>(size());
    final int pseudoHeadersEnd = pseudoHeaders.length - 1;
    for (int i = 0; i < pseudoHeadersEnd; i += 2) {
      names.add(pseudoHeaders[i]);
    }

    final int otherHeadersEnd = otherHeaders.length - 1;
    for (int i = 0; i < otherHeadersEnd; i += 2) {
      names.add(otherHeaders[i]);
    }
    return names;
  }

  @Override
  public Http2Headers add(final CharSequence name, final CharSequence value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers add(final CharSequence name, final Iterable<? extends CharSequence> values) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers add(final CharSequence name, final CharSequence... values) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addObject(final CharSequence name, final Object value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addObject(final CharSequence name, final Iterable<?> values) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addObject(final CharSequence name, final Object... values) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addBoolean(final CharSequence name, final boolean value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addByte(final CharSequence name, final byte value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addChar(final CharSequence name, final char value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addShort(final CharSequence name, final short value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addInt(final CharSequence name, final int value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addLong(final CharSequence name, final long value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addFloat(final CharSequence name, final float value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addDouble(final CharSequence name, final double value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers addTimeMillis(final CharSequence name, final long value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers add(
      final Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers set(final CharSequence name, final CharSequence value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers set(final CharSequence name, final Iterable<? extends CharSequence> values) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers set(final CharSequence name, final CharSequence... values) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setObject(final CharSequence name, final Object value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setObject(final CharSequence name, final Iterable<?> values) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setObject(final CharSequence name, final Object... values) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setBoolean(final CharSequence name, final boolean value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setByte(final CharSequence name, final byte value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setChar(final CharSequence name, final char value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setShort(final CharSequence name, final short value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setInt(final CharSequence name, final int value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setLong(final CharSequence name, final long value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setFloat(final CharSequence name, final float value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setDouble(final CharSequence name, final double value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setTimeMillis(final CharSequence name, final long value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers set(
      final Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers setAll(
      final Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public boolean remove(final CharSequence name) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers clear() {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Iterator<Map.Entry<CharSequence, CharSequence>> iterator() {
    return new ReadOnlyIterator();
  }

  @Override
  public Iterator<CharSequence> valueIterator(final CharSequence name) {
    return new ReadOnlyValueIterator(name);
  }

  @Override
  public Http2Headers method(final CharSequence value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers scheme(final CharSequence value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers authority(final CharSequence value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers path(final CharSequence value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public Http2Headers status(final CharSequence value) {
    throw new UnsupportedOperationException("read only");
  }

  @Override
  public CharSequence method() {
    return get(PseudoHeaderName.METHOD.value());
  }

  @Override
  public CharSequence scheme() {
    return get(PseudoHeaderName.SCHEME.value());
  }

  @Override
  public CharSequence authority() {
    return get(PseudoHeaderName.AUTHORITY.value());
  }

  @Override
  public CharSequence path() {
    return get(PseudoHeaderName.PATH.value());
  }

  @Override
  public CharSequence status() {
    return get(PseudoHeaderName.STATUS.value());
  }

  @Override
  public boolean contains(
      final CharSequence name, final CharSequence value, final boolean caseInsensitive) {
    final int nameHash = AsciiString.hashCode(name);
    final HashingStrategy<CharSequence> strategy =
        caseInsensitive ? CASE_INSENSITIVE_HASHER : CASE_SENSITIVE_HASHER;
    final int valueHash = strategy.hashCode(value);

    return contains(name, nameHash, value, valueHash, strategy, otherHeaders)
        || contains(name, nameHash, value, valueHash, strategy, pseudoHeaders);
  }

  private static boolean contains(
      final CharSequence name,
      final int nameHash,
      final CharSequence value,
      final int valueHash,
      final HashingStrategy<CharSequence> hashingStrategy,
      final AsciiString[] headers) {
    final int headersEnd = headers.length - 1;
    for (int i = 0; i < headersEnd; i += 2) {
      final AsciiString roName = headers[i];
      final AsciiString roValue = headers[i + 1];
      if (roName.hashCode() == nameHash
          && roValue.hashCode() == valueHash
          && roName.contentEqualsIgnoreCase(name)
          && hashingStrategy.equals(roValue, value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append('[');
    String separator = "";
    for (final Map.Entry<CharSequence, CharSequence> entry : this) {
      builder.append(separator);
      builder.append(entry.getKey()).append(": ").append(entry.getValue());
      separator = ", ";
    }
    return builder.append(']').toString();
  }

  private final class ReadOnlyValueIterator implements Iterator<CharSequence> {
    private int i;
    private final int nameHash;
    private final CharSequence name;
    private AsciiString[] current = pseudoHeaders.length != 0 ? pseudoHeaders : otherHeaders;
    private AsciiString next;

    ReadOnlyValueIterator(final CharSequence name) {
      nameHash = AsciiString.hashCode(name);
      this.name = name;
      calculateNext();
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public CharSequence next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      final CharSequence current = next;
      calculateNext();
      return current;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("read only");
    }

    private void calculateNext() {
      for (; i < current.length; i += 2) {
        final AsciiString roName = current[i];
        if (roName.hashCode() == nameHash && roName.contentEqualsIgnoreCase(name)) {
          next = current[i + 1];
          i += 2;
          return;
        }
      }
      if (i >= current.length && current == pseudoHeaders) {
        i = 0;
        current = otherHeaders;
        calculateNext();
      } else {
        next = null;
      }
    }
  }

  private final class ReadOnlyIterator
      implements Map.Entry<CharSequence, CharSequence>,
          Iterator<Map.Entry<CharSequence, CharSequence>> {
    private int i;
    private AsciiString[] current = pseudoHeaders.length != 0 ? pseudoHeaders : otherHeaders;
    private AsciiString key;
    private AsciiString value;

    @Override
    public boolean hasNext() {
      return i != current.length;
    }

    @Override
    public Map.Entry<CharSequence, CharSequence> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      key = current[i];
      value = current[i + 1];
      i += 2;
      if (i == current.length && current == pseudoHeaders) {
        current = otherHeaders;
        i = 0;
      }
      return this;
    }

    @Override
    public CharSequence getKey() {
      return key;
    }

    @Override
    public CharSequence getValue() {
      return value;
    }

    @Override
    public CharSequence setValue(final CharSequence value) {
      throw new UnsupportedOperationException("read only");
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("read only");
    }

    @Override
    public String toString() {
      return key.toString() + '=' + value.toString();
    }
  }
}
