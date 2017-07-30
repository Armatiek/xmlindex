package nl.armatiek.xmlindex.lucene.codec;

import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.lucene62.Lucene62Codec;

import nl.armatiek.xmlindex.lucene.codec.XMLIndexStoredFieldsFormat.Mode;

public class XMLIndexCodec extends FilterCodec {

  private final StoredFieldsFormat storedFieldsFormat;

  public XMLIndexCodec() {
    this(Mode.NO_COMPRESSION);
  }
  
  public XMLIndexCodec(Mode mode) {
    super("XMLIndexCodec", new Lucene62Codec());
    storedFieldsFormat = new XMLIndexStoredFieldsFormat(mode);
  }

  @Override
  public StoredFieldsFormat storedFieldsFormat() {
    return storedFieldsFormat;
  }
}