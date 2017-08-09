package nl.armatiek.xmlindex.saxon.functionset;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.registry.BuiltInFunctionSet;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.conf.WebDefinitions;

public class JsonFunctionSet extends BuiltInFunctionSet {

  private static JsonFunctionSet THE_INSTANCE = new JsonFunctionSet();
  
  public static JsonFunctionSet getInstance() {
    return THE_INSTANCE;
  }

  private JsonFunctionSet() {
    init();
  }

  private void init() {
    register("escape", 1, EscapeFn.class, BuiltInAtomicType.STRING, OPT, 0, LATE).arg(0, BuiltInAtomicType.STRING, OPT, EMPTY);
    register("unescape", 1, UnescapeFn.class, BuiltInAtomicType.STRING, OPT, 0, LATE).arg(0, BuiltInAtomicType.STRING, OPT, EMPTY);
  }

  @Override
  public String getNamespace() {
    return WebDefinitions.NAMESPACE_XMLINDEX_FUNCTIONS_JSON;
  }

  @Override
  public String getConventionalPrefix() {
    return "json";
  }
  
  public static class EscapeFn extends SystemFunction {
    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      String json = ((StringValue) arguments[0].head()).getStringValue();
      if (StringUtils.isBlank(json))
        return ZeroOrOne.empty();
      return new ZeroOrOne<StringValue>(new StringValue(StringEscapeUtils.escapeJson(json)));
    }
  }
  
  public static class UnescapeFn extends SystemFunction {
    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      String json = ((StringValue) arguments[0].head()).getStringValue();
      if (StringUtils.isBlank(json))
        return ZeroOrOne.empty();
      return new ZeroOrOne<StringValue>(new StringValue(StringEscapeUtils.unescapeJson(json)));  
    }
  }
  
}