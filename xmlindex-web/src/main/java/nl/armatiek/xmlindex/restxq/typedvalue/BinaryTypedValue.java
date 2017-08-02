package nl.armatiek.xmlindex.restxq.typedvalue;

import org.exquery.xdm.type.AbstractTypedValue;

import net.sf.saxon.value.Base64BinaryValue;
import nl.armatiek.xmlindex.restxq.adapter.TypeAdapter;

public class BinaryTypedValue extends AbstractTypedValue<Base64BinaryValue> {

  public BinaryTypedValue(final Base64BinaryValue value) {
    super(TypeAdapter.toExQueryType(value.getPrimitiveType()), value);
  }
  
}
