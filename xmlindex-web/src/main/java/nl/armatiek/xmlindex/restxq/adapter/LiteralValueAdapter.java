package nl.armatiek.xmlindex.restxq.adapter;

import org.exquery.xquery.Literal;
import org.exquery.xquery.Type;

import net.sf.saxon.value.AtomicValue;

public class LiteralValueAdapter implements Literal {

  private Type type;
  private String value;

  public LiteralValueAdapter(final AtomicValue literalValue) {
    this.type = TypeAdapter.toExQueryType(literalValue.getPrimitiveType());
    this.value = literalValue.getStringValue();
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getValue() {
    return value;
  }
  
}