package nl.armatiek.xmlindex.lucene.codec;

import org.apache.lucene.codecs.compressing.CompressionMode;
import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.codecs.compressing.Decompressor;

public class NoCompressionMode extends CompressionMode {
  
  @Override
  public Compressor newCompressor() {
    return new NoCompressionCompressor();
  }

  @Override
  public Decompressor newDecompressor() {
    return new NoCompressionDecompressor();
  }
  
  @Override
  public String toString() {
    return "NO_COMPRESSION";
  }

}