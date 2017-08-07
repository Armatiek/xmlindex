package nl.armatiek.xmlindex.saxon.functionset;

import javax.servlet.http.HttpServletRequest;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.registry.BuiltInFunctionSet;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.conf.WebDefinitions;

public class RequestFunctionSet extends BuiltInFunctionSet {

  private static RequestFunctionSet THE_INSTANCE = new RequestFunctionSet();
  
  private static final StructuredQName PARAM_NAME_REQUEST = new StructuredQName("", WebDefinitions.NAMESPACE_REQUEST, "request");

  public static RequestFunctionSet getInstance() {
    return THE_INSTANCE;
  }

  private RequestFunctionSet() {
    init();
  }

  /*
   * public BuiltInFunctionSet.Entry arg(int a,
                                    ItemType type,
                                    int options,
                                    Sequence resultIfEmpty)
                                    
   * Add information to a function entry about the argument types of the function
   * Parameters:
   *   a - the position of the argument, counting from zero
   *   type - the item type of the argument
   *   options - the cardinality and usage of the argument
   *   resultIfEmpty - the value returned by the function if an empty sequence appears as the value of this argument, 
   *     in the case when this result is unaffected by any other arguments. Supply null if this does not apply.
   */
  
  private void init() {
    // register("node-set", 1, NodeSetFn.class, AnyItemType.getInstance(), OPT, 0, 0).arg(0, AnyItemType.getInstance(), OPT, EMPTY);
    register("method", 0, MethodFn.class, BuiltInAtomicType.STRING, ONE, 0, 0).arg(0, AnyItemType.getInstance(), ONE, null);
  }

  @Override
  public String getNamespace() {
    return WebDefinitions.NAMESPACE_REQUEST;
  }

  @Override
  public String getConventionalPrefix() {
    return "request";
  }

  /*
  public static class NodeSetFn extends SystemFunction {
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
      return arguments[0];
    }
  }
  */
  
  public static class MethodFn extends SystemFunction {
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getMethod());
    }
  }
  
  private static HttpServletRequest getRequest(XPathContext context) {
    return (HttpServletRequest) ((ObjectValue<?>) context.getController().getParameter(PARAM_NAME_REQUEST)).getObject();
  }
  
}