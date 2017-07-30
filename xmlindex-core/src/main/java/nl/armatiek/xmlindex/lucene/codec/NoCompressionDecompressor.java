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
