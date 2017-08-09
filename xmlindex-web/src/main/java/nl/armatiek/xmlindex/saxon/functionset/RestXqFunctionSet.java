package nl.armatiek.xmlindex.saxon.functionset;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.registry.BuiltInFunctionSet;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.conf.WebDefinitions;

public class RestXqFunctionSet extends BuiltInFunctionSet {

  private static RestXqFunctionSet THE_INSTANCE = new RestXqFunctionSet();
  
  private static final StructuredQName PARAM_NAME_REQUEST = new StructuredQName("", WebDefinitions.NAMESPACE_REQUEST, "request");

  public static RestXqFunctionSet getInstance() {
    return THE_INSTANCE;
  }

  private RestXqFunctionSet() {
    init();
  }

  private void init() {
    register("base-uri", 0, BaseUriFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("uri", 0, UriFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
  }

  @Override
  public String getNamespace() {
    return WebDefinitions.NAMESPACE_RESTXQ;
  }

  @Override
  public String getConventionalPrefix() {
    return "rest";
  }
  
  public static class BaseUriFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      HttpServletRequest req = getRequest(context);
      return new StringValue(req.getRequestURI().replace(StringUtils.defaultIfBlank(req.getPathInfo(), ""), ""));
    }
  }
  
  public static class UriFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getRequestURI());
    }
  }
  
  private static HttpServletRequest getRequest(XPathContext context) {
    return (HttpServletRequest) ((ObjectValue<?>) context.getController().getParameter(PARAM_NAME_REQUEST)).getObject();
  }
  
}