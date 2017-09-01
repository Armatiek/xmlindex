/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.armatiek.xmlindex.lucene.codec;

import java.io.IOException;

import org.apache.lucene.codecs.compressing.Decompressor;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;

public class NoCompressionDecompressor extends Decompressor {

  @Override
  public void decompress(DataInput in, int originalLength, int offset, int length, BytesRef bytes) throws IOException {
    if (length == 0) {
      bytes.length = 0;
      return;
    }
    if (bytes.bytes.length < originalLength)
      bytes.bytes = new byte[ArrayUtil.oversize(originalLength, 1)];
    in.readBytes(bytes.bytes, 0, offset + length);
    bytes.offset = offset;
    bytes.length = length;
  }
  
  @Override
  public Decompressor clone() {
    return this;
  }
  
}
