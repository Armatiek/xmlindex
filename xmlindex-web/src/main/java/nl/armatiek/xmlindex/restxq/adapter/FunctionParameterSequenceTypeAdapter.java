package nl.armatiek.xmlindex.restxq.adapter;

import org.exquery.xquery.Cardinality;
import org.exquery.xquery.FunctionArgument;
import org.exquery.xquery.Type;

import net.sf.saxon.expr.instruct.UserFunctionParameter;
import net.sf.saxon.value.SequenceType;

public class FunctionParameterSequenceTypeAdapter implements FunctionArgument {
  
  private String name;
  private Type primaryType;
  private Cardinality cardinality;

  public FunctionParameterSequenceTypeAdapter(final UserFunctionParameter param, final SequenceType sequenceType) {
    this.name = param.getVariableQName().getLocalPart();
    this.primaryType = TypeAdapter.toExQueryType(sequenceType.getPrimaryType()); // TODO
    this.cardinality = CardinalityAdapter.getCardinality(sequenceType.getCardinality());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Type getType() {
    return primaryType;
  }

  @Override
  public Cardinality getCardinality() {
    return cardinality;
  }
  
}