package nl.armatiek.xmlindex.restxq;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.exquery.http.HttpResponse;
import org.exquery.restxq.RestXqServiceException;
import org.exquery.restxq.impl.serialization.AbstractRestXqServiceSerializer;
import org.exquery.restxq.impl.serialization.SerializationProperty;
import org.exquery.xquery.Sequence;
import org.exquery.xquery.Type;
import org.exquery.xquery.TypedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.value.Base64BinaryValue;
import nl.armatiek.xmlindex.restxq.adapter.SequenceAdapter;

public class RestXqServiceSerializerImpl extends AbstractRestXqServiceSerializer {

  private static final Logger logger = LoggerFactory.getLogger(RestXqServiceSerializerImpl.class);
  
  private Processor processor;
  
  public RestXqServiceSerializerImpl(Processor processor) { 
    this.processor = processor;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected void serializeBinaryBody(final Sequence result, final HttpResponse response) throws RestXqServiceException {
    for (final TypedValue typedValue : (Iterable<TypedValue>) result) {
      if (typedValue.getType() == Type.BASE64_BINARY || typedValue.getType() == Type.HEX_BINARY) {
        final Base64BinaryValue binaryValue = (Base64BinaryValue) typedValue.getValue();
        try (final OutputStream os = response.getOutputStream()) {
          os.write(binaryValue.getBinaryValue());
        } catch (final IOException ioe) {
          throw new RestXqServiceException("Error while serializing binary: " + ioe.toString(), ioe);
        }
        return; // TODO support more than one binary result -- multipart?
      } else {
        throw new RestXqServiceException("Expected binary value, but found: " + typedValue.getType().name());
      }
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  protected void serializeNodeBody(final Sequence result, final HttpResponse response, final Map<SerializationProperty, String> serializationProperties) throws RestXqServiceException {
    try (final Writer writer = new OutputStreamWriter(response.getOutputStream(), serializationProperties.get(SerializationProperty.ENCODING))) {
      final Properties outputProperties = serializationPropertiesToProperties(serializationProperties);
      Serializer serializer = processor.newSerializer(writer);
      Enumeration<?> e = outputProperties.propertyNames();
      while (e.hasMoreElements()) {
        String key = ((String) e.nextElement()).replace('_', '-');
        try {
          serializer.setOutputProperty(new QName(key), outputProperties.getProperty(key));
        } catch (IllegalArgumentException iae) {
          logger.warn("Unsupported serialization property \"" + key + "\"", iae);
        }
      }
      serializer.serializeXdmValue(((SequenceAdapter) result).getSaxonXdmValue());
      writer.flush();
    } catch (IOException | SaxonApiException e) {
      throw new RestXqServiceException("Error while serializing xml: " + e.toString(), e);
    }
  }

  private Properties serializationPropertiesToProperties(final Map<SerializationProperty, String> serializationProperties) {
    final Properties props = new Properties();
    for (final Entry<SerializationProperty, String> serializationProperty : serializationProperties.entrySet()) {
      if (serializationProperty.getKey() == SerializationProperty.OMIT_XML_DECLARATION) {
        // TODO why are not all keys transformed from '_' to '-'? I have a
        // feeling we did something special for MEDIA_TYPE???
        props.setProperty(serializationProperty.getKey().name().toLowerCase().replace('_', '-'), serializationProperty.getValue());
      } else {
        props.setProperty(serializationProperty.getKey().name().toLowerCase(), serializationProperty.getValue());
      }
    }
    return props;
  }
  
}