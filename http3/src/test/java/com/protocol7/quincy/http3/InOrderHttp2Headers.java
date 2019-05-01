/*
 * Copyright 2017 The Netty Project
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

package com.protocol7.quincy.http3;

import static io.netty.util.AsciiString.CASE_INSENSITIVE_HASHER;
import static io.netty.util.AsciiString.CASE_SENSITIVE_HASHER;

import io.netty.handler.codec.CharSequenceValueConverter;
import io.netty.handler.codec.DefaultHeaders;

/** Http2Headers implementation that preserves headers insertion order. */
public class InOrderHttp2Headers extends DefaultHeaders<CharSequence, CharSequence, Http2Headers>
    implements Http2Headers {

  InOrderHttp2Headers() {
    super(CharSequenceValueConverter.INSTANCE);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof Http2Headers && equals((Http2Headers) o, CASE_SENSITIVE_HASHER);
  }

  @Override
  public int hashCode() {
    return hashCode(CASE_SENSITIVE_HASHER);
  }

  @Override
  public Http2Headers method(final CharSequence value) {
    set(PseudoHeaderName.METHOD.value(), value);
    return this;
  }

  @Override
  public Http2Headers scheme(final CharSequence value) {
    set(PseudoHeaderName.SCHEME.value(), value);
    return this;
  }

  @Override
  public Http2Headers authority(final CharSequence value) {
    set(PseudoHeaderName.AUTHORITY.value(), value);
    return this;
  }

  @Override
  public Http2Headers path(final CharSequence value) {
    set(PseudoHeaderName.PATH.value(), value);
    return this;
  }

  @Override
  public Http2Headers status(final CharSequence value) {
    set(PseudoHeaderName.STATUS.value(), value);
    return this;
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
    return contains(name, value, caseInsensitive ? CASE_INSENSITIVE_HASHER : CASE_SENSITIVE_HASHER);
  }
}
