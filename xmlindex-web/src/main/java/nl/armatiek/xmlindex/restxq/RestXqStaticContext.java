package nl.armatiek.xmlindex.restxq;

import java.util.Map;

import org.exquery.restxq.RestXqServiceRegistry;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;

public class RestXqStaticContext {
  
  private final Processor processor;
  private final Configuration configuration;
  private final XQueryExecutable restXQuery;
  private final XdmItem contextItem;
  private final Map<QName, XdmValue> externalVariables;
  private final RestXqServiceRegistry serviceRegistry;
  
  public RestXqStaticContext(Processor processor, Configuration configuration, 
      XQueryExecutable restXQuery, XdmItem contextItem, Map<QName, XdmValue> externalVariables, 
      RestXqServiceRegistry serviceRegistry) {
    this.processor = processor;
    this.configuration = configuration;
    this.restXQuery = restXQuery;
    this.contextItem = contextItem;
    this.externalVariables = externalVariables;
    this.serviceRegistry = serviceRegistry;
  }

  public Processor getProcessor() {
    return processor;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public XQueryExecutable getRestXQuery() {
    return restXQuery;
  }
  
  public XdmItem getContextItem() {
    return contextItem;
  }

  public Map<QName, XdmValue> getExternalVariables() {
    return externalVariables;
  }

  public RestXqServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

}