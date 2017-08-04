package nl.armatiek.xmlindex.restxq.adapter;

import org.exquery.xquery.Cardinality;

import net.sf.saxon.expr.StaticProperty;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class CardinalityAdapter {

  public static Cardinality getCardinality(int cardinality) {
    switch (cardinality) {
      case  StaticProperty.ALLOWS_ZERO_OR_ONE:
        return Cardinality.ZERO_OR_ONE;
      case  StaticProperty.EXACTLY_ONE:
        return Cardinality.ONE;
      case  StaticProperty.ALLOWS_ZERO_OR_MORE:
        return Cardinality.ZERO_OR_MORE;
      case  StaticProperty.ALLOWS_ONE_OR_MORE:
        return Cardinality.ONE_OR_MORE;
      case  StaticProperty.ALLOWS_ZERO:
        return Cardinality.ZERO;
    }
    throw new XMLIndexException("Could not convert Saxon cardinality " + cardinality + " to EXQuery cardinality");
  }
  
}
