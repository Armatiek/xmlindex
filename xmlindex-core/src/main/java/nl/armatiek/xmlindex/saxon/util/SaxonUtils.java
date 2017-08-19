package nl.armatiek.xmlindex.saxon.util;

import net.sf.saxon.expr.parser.Token;

public class SaxonUtils {
  
  public static String token2String(int operator) {
    switch (operator) {
      case Token.FEQ:
      case Token.EQUALS:
        return "=";
      case Token.FGT:
      case Token.GT:
        return ">";
      case Token.FGE:
      case Token.GE:
        return ">=";
      case Token.FLT:  
      case Token.LT:
        return "<";
      case Token.FLE:
      case Token.LE:
        return "<=";
      default:
        return "Unknown: " + Integer.toString(operator);
    }
  }

}