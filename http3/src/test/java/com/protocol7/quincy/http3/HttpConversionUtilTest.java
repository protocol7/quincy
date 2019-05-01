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

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.TE;
import static io.netty.handler.codec.http.HttpHeaderValues.GZIP;
import static io.netty.handler.codec.http.HttpHeaderValues.TRAILERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;
import org.junit.Test;

public class HttpConversionUtilTest {
  @Test
  public void setHttp2AuthorityWithoutUserInfo() {
    final Http2Headers headers = new DefaultHttp2Headers();

    HttpConversionUtil.setHttp2Authority("foo", headers);
    assertEquals(new AsciiString("foo"), headers.authority());
  }

  @Test
  public void setHttp2AuthorityWithUserInfo() {
    final Http2Headers headers = new DefaultHttp2Headers();

    HttpConversionUtil.setHttp2Authority("info@foo", headers);
    assertEquals(new AsciiString("foo"), headers.authority());

    HttpConversionUtil.setHttp2Authority("@foo.bar", headers);
    assertEquals(new AsciiString("foo.bar"), headers.authority());
  }

  @Test
  public void setHttp2AuthorityNullOrEmpty() {
    final Http2Headers headers = new DefaultHttp2Headers();

    HttpConversionUtil.setHttp2Authority(null, headers);
    assertNull(headers.authority());

    HttpConversionUtil.setHttp2Authority("", headers);
    assertSame(AsciiString.EMPTY_STRING, headers.authority());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setHttp2AuthorityWithEmptyAuthority() {
    HttpConversionUtil.setHttp2Authority("info@", new DefaultHttp2Headers());
  }

  @Test
  public void stripTEHeaders() {
    final HttpHeaders inHeaders = new DefaultHttpHeaders();
    inHeaders.add(TE, GZIP);
    final Http2Headers out = new DefaultHttp2Headers();
    HttpConversionUtil.toHttp2Headers(inHeaders, out);
    assertTrue(out.isEmpty());
  }

  @Test
  public void stripTEHeadersExcludingTrailers() {
    final HttpHeaders inHeaders = new DefaultHttpHeaders();
    inHeaders.add(TE, GZIP);
    inHeaders.add(TE, TRAILERS);
    final Http2Headers out = new DefaultHttp2Headers();
    HttpConversionUtil.toHttp2Headers(inHeaders, out);
    assertSame(TRAILERS, out.get(TE));
  }

  @Test
  public void stripTEHeadersCsvSeparatedExcludingTrailers() {
    final HttpHeaders inHeaders = new DefaultHttpHeaders();
    inHeaders.add(TE, GZIP + "," + TRAILERS);
    final Http2Headers out = new DefaultHttp2Headers();
    HttpConversionUtil.toHttp2Headers(inHeaders, out);
    assertSame(TRAILERS, out.get(TE));
  }

  @Test
  public void stripTEHeadersCsvSeparatedAccountsForValueSimilarToTrailers() {
    final HttpHeaders inHeaders = new DefaultHttpHeaders();
    inHeaders.add(TE, GZIP + "," + TRAILERS + "foo");
    final Http2Headers out = new DefaultHttp2Headers();
    HttpConversionUtil.toHttp2Headers(inHeaders, out);
    assertFalse(out.contains(TE));
  }

  @Test
  public void stripTEHeadersAccountsForValueSimilarToTrailers() {
    final HttpHeaders inHeaders = new DefaultHttpHeaders();
    inHeaders.add(TE, TRAILERS + "foo");
    final Http2Headers out = new DefaultHttp2Headers();
    HttpConversionUtil.toHttp2Headers(inHeaders, out);
    assertFalse(out.contains(TE));
  }

  @Test
  public void stripTEHeadersAccountsForOWS() {
    final HttpHeaders inHeaders = new DefaultHttpHeaders();
    inHeaders.add(TE, " " + TRAILERS + " ");
    final Http2Headers out = new DefaultHttp2Headers();
    HttpConversionUtil.toHttp2Headers(inHeaders, out);
    assertSame(TRAILERS, out.get(TE));
  }

  @Test
  public void stripConnectionHeadersAndNominees() {
    final HttpHeaders inHeaders = new DefaultHttpHeaders();
    inHeaders.add(CONNECTION, "foo");
    inHeaders.add("foo", "bar");
    final Http2Headers out = new DefaultHttp2Headers();
    HttpConversionUtil.toHttp2Headers(inHeaders, out);
    assertTrue(out.isEmpty());
  }

  @Test
  public void stripConnectionNomineesWithCsv() {
    final HttpHeaders inHeaders = new DefaultHttpHeaders();
    inHeaders.add(CONNECTION, "foo,  bar");
    inHeaders.add("foo", "baz");
    inHeaders.add("bar", "qux");
    inHeaders.add("hello", "world");
    final Http2Headers out = new DefaultHttp2Headers();
    HttpConversionUtil.toHttp2Headers(inHeaders, out);
    assertEquals(1, out.size());
    assertSame("world", out.get("hello"));
  }
}
