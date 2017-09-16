package nl.armatiek.xmlindex.restxq;

import java.util.HashMap;
import java.util.Map;

import org.exquery.http.HttpRequest;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;

public class RestXqDynamicContext {
  
  private final RestXqStaticContext staticContext;
  private final HttpRequest request;
  private final Map<QName, XdmValue> externalVariables;
  private final XdmItem contextItem;
  private final boolean developmentMode;
  
  public RestXqDynamicContext(RestXqStaticContext staticContext, HttpRequest request, 
      Map<QName, XdmValue> externalVariables, XdmItem contextItem, boolean developmentMode) {
    this.staticContext = staticContext;
    this.request = request;
    this.externalVariables = externalVariables;
    this.contextItem = contextItem;
    this.developmentMode = developmentMode;
  }
  
  public RestXqDynamicContext(RestXqStaticContext staticContext, HttpRequest request, 
      Map<QName, XdmValue> externalVariables, boolean developmentMode) {
    this(staticContext, request, externalVariables, null, developmentMode);
  }
  
  public boolean getDevelopmentMode() {
    return developmentMode;
  }
  
  public RestXqStaticContext getStaticContext() {
    return staticContext;
  }
  
  public HttpRequest getRequest() {
    return request;
  }

  public Map<QName, XdmValue> getExternalVariables() {
    return externalVariables;
  }
  
  public XdmItem getContextItem() {
    return this.contextItem;
  }
  
  public Map<QName, XdmValue> getAllExternalVariables() {
    Map<QName, XdmValue> allVars = new HashMap<QName, XdmValue>();
    if (staticContext.getExternalVariables() != null)
      allVars.putAll(staticContext.getExternalVariables());
    if (externalVariables != null)
      allVars.putAll(externalVariables);
    return allVars;
  }
  
}