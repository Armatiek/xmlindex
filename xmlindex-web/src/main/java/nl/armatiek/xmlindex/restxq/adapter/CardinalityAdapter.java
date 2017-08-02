package nl.armatiek.xmlindex.restxq.adapter;

import org.exquery.xquery.Cardinality;

public class CardinalityAdapter {

  public static Cardinality getCardinality(int cardinality) {
    switch (cardinality) {
    case 1:
      return Cardinality.ZERO_OR_ONE;
    case 2:
      return Cardinality.ONE;
    case 3:
      return Cardinality.ZERO_OR_MORE;
    case 4:
      return Cardinality.ONE_OR_MORE;
    case 5:
      return Cardinality.ZERO;
    }
    return null;
  }
  
}