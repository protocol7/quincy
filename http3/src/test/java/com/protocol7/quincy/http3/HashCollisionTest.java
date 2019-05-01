/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.protocol7.quincy.http3;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.AsciiString;
import io.netty.util.internal.PlatformDependent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Ignore;

@Ignore
public final class HashCollisionTest {
  private HashCollisionTest() {}

  public static void main(final String[] args)
      throws IllegalAccessException, IOException, URISyntaxException {
    // Big initial size for when all name sources are pulled in.
    final List<CharSequence> strings = new ArrayList<CharSequence>(350000);
    addHttpHeaderNames(strings);
    addHttpHeaderValues(strings);
    addHttp2HeaderNames(strings);
    addWordsFromFile(new File("/usr/share/dict/words"), strings);
    // More "english words" can be found here:
    // https://gist.github.com/Scottmitch/de2f03912778016ecee3c140478f07e0#file-englishwords-txt

    Map<Integer, List<CharSequence>> dups =
        calculateDuplicates(
            strings,
            new Function<CharSequence, Integer>() {
              @Override
              public Integer apply(final CharSequence string) {
                int h = 0;
                for (int i = 0; i < string.length(); ++i) {
                  // masking with 0x1F reduces the number of overall bits that impact the hash code
                  // but makes the hash
                  // code the same regardless of character case (upper case or lower case hash is
                  // the same).
                  h = h * 31 + (string.charAt(i) & 0x1F);
                }
                return h;
              }
            });
    final PrintStream writer = System.out;
    writer.println("==Old Duplicates==");
    printResults(writer, dups);

    dups =
        calculateDuplicates(
            strings,
            new Function<CharSequence, Integer>() {
              @Override
              public Integer apply(final CharSequence string) {
                return PlatformDependent.hashCodeAscii(string);
              }
            });
    writer.println();
    writer.println("==New Duplicates==");
    printResults(writer, dups);
  }

  private static void addHttpHeaderNames(final List<CharSequence> values)
      throws IllegalAccessException {
    for (final Field f : HttpHeaderNames.class.getFields()) {
      if (f.getType() == AsciiString.class) {
        values.add((AsciiString) f.get(null));
      }
    }
  }

  private static void addHttpHeaderValues(final List<CharSequence> values)
      throws IllegalAccessException {
    for (final Field f : HttpHeaderValues.class.getFields()) {
      if (f.getType() == AsciiString.class) {
        values.add((AsciiString) f.get(null));
      }
    }
  }

  private static void addHttp2HeaderNames(final List<CharSequence> values)
      throws IllegalAccessException {
    for (final Http2Headers.PseudoHeaderName name : Http2Headers.PseudoHeaderName.values()) {
      values.add(name.value());
    }
  }

  private static void addWordsFromFile(final File file, final List<CharSequence> values)
      throws IllegalAccessException, IOException {
    final BufferedReader br = new BufferedReader(new FileReader(file));
    try {
      String line;
      while ((line = br.readLine()) != null) {
        // Make a "best effort" to prune input which contains characters that are not valid in HTTP
        // header names
        if (line.indexOf('\'') < 0) {
          values.add(line);
        }
      }
    } finally {
      br.close();
    }
  }

  private static Map<Integer, List<CharSequence>> calculateDuplicates(
      final List<CharSequence> strings, final Function<CharSequence, Integer> hasher) {
    final Map<Integer, List<CharSequence>> hashResults = new HashMap<Integer, List<CharSequence>>();
    final Set<Integer> duplicateHashCodes = new HashSet<Integer>();

    for (final CharSequence str : strings) {
      final Integer hash = hasher.apply(str);
      List<CharSequence> results = hashResults.get(hash);
      if (results == null) {
        results = new ArrayList<CharSequence>(1);
        hashResults.put(hash, results);
      } else {
        duplicateHashCodes.add(hash);
      }
      results.add(str);
    }

    if (duplicateHashCodes.isEmpty()) {
      return Collections.emptyMap();
    }
    final Map<Integer, List<CharSequence>> duplicates =
        new HashMap<Integer, List<CharSequence>>(duplicateHashCodes.size());
    for (final Integer duplicateHashCode : duplicateHashCodes) {
      final List<CharSequence> realDups = new ArrayList<CharSequence>(2);
      final Iterator<CharSequence> itr = hashResults.get(duplicateHashCode).iterator();
      // there should be at least 2 elements in the list ... bcz there may be duplicates
      realDups.add(itr.next());
      checknext:
      do {
        final CharSequence next = itr.next();
        for (final CharSequence potentialDup : realDups) {
          if (!AsciiString.contentEqualsIgnoreCase(next, potentialDup)) {
            realDups.add(next);
            break checknext;
          }
        }
      } while (itr.hasNext());

      if (realDups.size() > 1) {
        duplicates.put(duplicateHashCode, realDups);
      }
    }
    return duplicates;
  }

  private static void printResults(
      final PrintStream stream, final Map<Integer, List<CharSequence>> dups) {
    stream.println("Number duplicates: " + dups.size());
    for (final Entry<Integer, List<CharSequence>> entry : dups.entrySet()) {
      stream.print(
          entry.getValue().size() + " duplicates for hash: " + entry.getKey() + " values: ");
      for (final CharSequence str : entry.getValue()) {
        stream.print("[" + str + "] ");
      }
      stream.println();
    }
  }

  private interface Function<P, R> {
    R apply(P param);
  }
}
