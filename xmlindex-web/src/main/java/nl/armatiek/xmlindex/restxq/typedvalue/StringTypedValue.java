package nl.armatiek.xmlindex.restxq.typedvalue;

import org.exquery.xdm.type.AbstractTypedValue;
import org.exquery.xquery.Type;

import net.sf.saxon.value.StringValue;

public class StringTypedValue extends AbstractTypedValue<StringValue> {

  public StringTypedValue(final StringValue stringValue) {
    super(Type.STRING, stringValue);
  }
  
}
