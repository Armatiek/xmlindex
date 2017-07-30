package nl.armatiek.xmlindex.lucene.codec;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.codecs.StoredFieldsWriter;
import org.apache.lucene.codecs.compressing.CompressingStoredFieldsFormat;
import org.apache.lucene.codecs.compressing.CompressionMode;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;

public final class XMLIndexStoredFieldsFormat extends StoredFieldsFormat {
  
  public static enum Mode {
    NO_COMPRESSION,
    BEST_SPEED,
    BEST_COMPRESSION
  }
  
  public static final String MODE_KEY = XMLIndexStoredFieldsFormat.class.getSimpleName() + ".mode";
  
  final Mode mode;
  
  public XMLIndexStoredFieldsFormat() {
    this(Mode.NO_COMPRESSION);
  }
  
  public XMLIndexStoredFieldsFormat(Mode mode) {
    this.mode = Objects.requireNonNull(mode);
  }

  @Override
  public StoredFieldsReader fieldsReader(Directory directory, SegmentInfo si, FieldInfos fn, IOContext context) throws IOException {
    String value = si.getAttribute(MODE_KEY);
    if (value == null) {
      throw new IllegalStateException("missing value for " + MODE_KEY + " for segment: " + si.name);
    }
    Mode mode = Mode.valueOf(value);
    return impl(mode).fieldsReader(directory, si, fn, context);
  }

  @Override
  public StoredFieldsWriter fieldsWriter(Directory directory, SegmentInfo si, IOContext context) throws IOException {
    String previous = si.putAttribute(MODE_KEY, mode.name());
    if (previous != null && previous.equals(mode.name()) == false) {
      throw new IllegalStateException("found existing value for " + MODE_KEY + " for segment: " + si.name +
                                      "old=" + previous + ", new=" + mode.name());
    }
    return impl(mode).fieldsWriter(directory, si, context);
  }
  
  StoredFieldsFormat impl(Mode mode) {
    switch (mode) {
      case NO_COMPRESSION:
        return new CompressingStoredFieldsFormat("Lucene50StoredFieldsUncompressed", new NoCompressionMode(), 1 << 14, 128, 1024);
      case BEST_SPEED: 
        return new CompressingStoredFieldsFormat("Lucene50StoredFieldsFast", CompressionMode.FAST, 1 << 14, 128, 1024);
      case BEST_COMPRESSION: 
        return new CompressingStoredFieldsFormat("Lucene50StoredFieldsHigh", CompressionMode.HIGH_COMPRESSION, 61440, 512, 1024);
      default: throw new AssertionError();
    }
  }
}
