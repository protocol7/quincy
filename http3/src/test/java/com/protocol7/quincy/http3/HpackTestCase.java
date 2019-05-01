/*
 * Copyright 2015 The Netty Project
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

/*
 * Copyright 2014 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.protocol7.quincy.http3;

import static com.protocol7.quincy.http3.Http2CodecUtil.DEFAULT_HEADER_LIST_SIZE;
import static com.protocol7.quincy.http3.Http2CodecUtil.MAX_HEADER_LIST_SIZE;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HpackTestCase {

  private static final Gson GSON =
      new GsonBuilder()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .registerTypeAdapter(HpackHeaderField.class, new HeaderFieldDeserializer())
          .create();

  int maxHeaderTableSize = -1;
  boolean sensitiveHeaders;

  List<HeaderBlock> headerBlocks;

  private HpackTestCase() {}

  static HpackTestCase load(final InputStream is) {
    final InputStreamReader r = new InputStreamReader(is);
    final HpackTestCase hpackTestCase = GSON.fromJson(r, HpackTestCase.class);
    for (final HeaderBlock headerBlock : hpackTestCase.headerBlocks) {
      headerBlock.encodedBytes = StringUtil.decodeHexDump(headerBlock.getEncodedStr());
    }
    return hpackTestCase;
  }

  void testCompress() throws Exception {
    final HpackEncoder hpackEncoder = createEncoder();

    for (final HeaderBlock headerBlock : headerBlocks) {

      final byte[] actual =
          encode(
              hpackEncoder,
              headerBlock.getHeaders(),
              headerBlock.getMaxHeaderTableSize(),
              sensitiveHeaders);

      if (!Arrays.equals(actual, headerBlock.encodedBytes)) {
        throw new AssertionError(
            "\nEXPECTED:\n"
                + headerBlock.getEncodedStr()
                + "\nACTUAL:\n"
                + StringUtil.toHexString(actual));
      }

      final List<HpackHeaderField> actualDynamicTable = new ArrayList<HpackHeaderField>();
      for (int index = 0; index < hpackEncoder.length(); index++) {
        actualDynamicTable.add(hpackEncoder.getHeaderField(index));
      }

      final List<HpackHeaderField> expectedDynamicTable = headerBlock.getDynamicTable();

      if (!expectedDynamicTable.equals(actualDynamicTable)) {
        throw new AssertionError(
            "\nEXPECTED DYNAMIC TABLE:\n"
                + expectedDynamicTable
                + "\nACTUAL DYNAMIC TABLE:\n"
                + actualDynamicTable);
      }

      if (headerBlock.getTableSize() != hpackEncoder.size()) {
        throw new AssertionError(
            "\nEXPECTED TABLE SIZE: "
                + headerBlock.getTableSize()
                + "\n ACTUAL TABLE SIZE : "
                + hpackEncoder.size());
      }
    }
  }

  void testDecompress() throws Exception {
    final HpackDecoder hpackDecoder = createDecoder();

    for (final HeaderBlock headerBlock : headerBlocks) {

      final List<HpackHeaderField> actualHeaders = decode(hpackDecoder, headerBlock.encodedBytes);

      final List<HpackHeaderField> expectedHeaders = new ArrayList<HpackHeaderField>();
      for (final HpackHeaderField h : headerBlock.getHeaders()) {
        expectedHeaders.add(new HpackHeaderField(h.name, h.value));
      }

      if (!expectedHeaders.equals(actualHeaders)) {
        throw new AssertionError("\nEXPECTED:\n" + expectedHeaders + "\nACTUAL:\n" + actualHeaders);
      }

      final List<HpackHeaderField> actualDynamicTable = new ArrayList<HpackHeaderField>();
      for (int index = 0; index < hpackDecoder.length(); index++) {
        actualDynamicTable.add(hpackDecoder.getHeaderField(index));
      }

      final List<HpackHeaderField> expectedDynamicTable = headerBlock.getDynamicTable();

      if (!expectedDynamicTable.equals(actualDynamicTable)) {
        throw new AssertionError(
            "\nEXPECTED DYNAMIC TABLE:\n"
                + expectedDynamicTable
                + "\nACTUAL DYNAMIC TABLE:\n"
                + actualDynamicTable);
      }

      if (headerBlock.getTableSize() != hpackDecoder.size()) {
        throw new AssertionError(
            "\nEXPECTED TABLE SIZE: "
                + headerBlock.getTableSize()
                + "\n ACTUAL TABLE SIZE : "
                + hpackDecoder.size());
      }
    }
  }

  private HpackEncoder createEncoder() {
    int maxHeaderTableSize = this.maxHeaderTableSize;
    if (maxHeaderTableSize == -1) {
      maxHeaderTableSize = Integer.MAX_VALUE;
    }

    try {
      return Http2TestUtil.newTestEncoder(true, MAX_HEADER_LIST_SIZE, maxHeaderTableSize);
    } catch (final Http2Exception e) {
      throw new Error("invalid initial values!", e);
    }
  }

  private HpackDecoder createDecoder() {
    int maxHeaderTableSize = this.maxHeaderTableSize;
    if (maxHeaderTableSize == -1) {
      maxHeaderTableSize = Integer.MAX_VALUE;
    }

    return new HpackDecoder(DEFAULT_HEADER_LIST_SIZE, 32, maxHeaderTableSize);
  }

  private static byte[] encode(
      final HpackEncoder hpackEncoder,
      final List<HpackHeaderField> headers,
      final int maxHeaderTableSize,
      final boolean sensitive)
      throws Http2Exception {
    final Http2Headers http2Headers = toHttp2Headers(headers);
    final Http2HeadersEncoder.SensitivityDetector sensitivityDetector =
        new Http2HeadersEncoder.SensitivityDetector() {
          @Override
          public boolean isSensitive(final CharSequence name, final CharSequence value) {
            return sensitive;
          }
        };
    final ByteBuf buffer = Unpooled.buffer();
    try {
      if (maxHeaderTableSize != -1) {
        hpackEncoder.setMaxHeaderTableSize(buffer, maxHeaderTableSize);
      }

      hpackEncoder.encodeHeaders(
          3 /* randomly chosen */, buffer, http2Headers, sensitivityDetector);
      final byte[] bytes = new byte[buffer.readableBytes()];
      buffer.readBytes(bytes);
      return bytes;
    } finally {
      buffer.release();
    }
  }

  private static Http2Headers toHttp2Headers(final List<HpackHeaderField> inHeaders) {
    final Http2Headers headers = new DefaultHttp2Headers(false);
    for (final HpackHeaderField e : inHeaders) {
      headers.add(e.name, e.value);
    }
    return headers;
  }

  private static List<HpackHeaderField> decode(
      final HpackDecoder hpackDecoder, final byte[] expected) throws Exception {
    final ByteBuf in = Unpooled.wrappedBuffer(expected);
    try {
      final List<HpackHeaderField> headers = new ArrayList<HpackHeaderField>();
      final TestHeaderListener listener = new TestHeaderListener(headers);
      hpackDecoder.decode(0, in, listener, true);
      return headers;
    } finally {
      in.release();
    }
  }

  private static String concat(final List<String> l) {
    final StringBuilder ret = new StringBuilder();
    for (final String s : l) {
      ret.append(s);
    }
    return ret.toString();
  }

  static class HeaderBlock {
    private int maxHeaderTableSize = -1;
    private byte[] encodedBytes;
    private List<String> encoded;
    private List<HpackHeaderField> headers;
    private List<HpackHeaderField> dynamicTable;
    private int tableSize;

    private int getMaxHeaderTableSize() {
      return maxHeaderTableSize;
    }

    public String getEncodedStr() {
      return concat(encoded).replaceAll(" ", "");
    }

    public List<HpackHeaderField> getHeaders() {
      return headers;
    }

    public List<HpackHeaderField> getDynamicTable() {
      return dynamicTable;
    }

    public int getTableSize() {
      return tableSize;
    }
  }

  static class HeaderFieldDeserializer implements JsonDeserializer<HpackHeaderField> {

    @Override
    public HpackHeaderField deserialize(
        final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
      final JsonObject jsonObject = json.getAsJsonObject();
      final Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
      if (entrySet.size() != 1) {
        throw new JsonParseException("JSON Object has multiple entries: " + entrySet);
      }
      final Map.Entry<String, JsonElement> entry = entrySet.iterator().next();
      final String name = entry.getKey();
      final String value = entry.getValue().getAsString();
      return new HpackHeaderField(name, value);
    }
  }
}
