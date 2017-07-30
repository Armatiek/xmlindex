package nl.armatiek.xmlindex.lucene.codec;

import java.io.IOException;

import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.store.DataOutput;

public class NoCompressionCompressor extends Compressor {
  
  @Override
  public void compress(byte[] bytes, int off, int len, DataOutput out) throws IOException {
    out.writeBytes(bytes, off, len);
  }
  
  @Override
  public void close() throws IOException { }

}
