package nl.armatiek.xmlindex.restxq;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exquery.http.HttpRequest;
import org.exquery.restxq.impl.RestXqServiceRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmValue;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletRequestAdapter;
import nl.armatiek.xmlindex.saxon.conf.XMLIndexWebInitializer;

public class IDERestXqServlet extends AbstractRestXqServlet {

  private static final long serialVersionUID = 1135079367879721547L;

  private static final Logger logger = LoggerFactory.getLogger(IDERestXqServlet.class);
  
  private RestXqStaticContext staticContext;
  
  protected synchronized void initStaticContext(HttpServletResponse resp) {
    if (staticContext != null)
      return;
    try {
      logger.info("Initializing RESTXQ static context ...");
      
      Configuration config = new Configuration();
      new XMLIndexWebInitializer().initialize(config);
      Processor processor = new Processor(config);
      
      RestXqServiceRegistryImpl registry = new RestXqServiceRegistryImpl();
      registry.addListener(new RestXqServiceRegistryLogger());
      
      File restXqFile = new File(context.getHomeDir(), "ide/ide.xqm");
      if (!restXqFile.isFile())
        throw new FileNotFoundException("RESTXQ module \"" + restXqFile + "\" not found");
    
      XQueryExecutable restXQuery = compileAndRegisterRestXQuery(restXqFile, registry, processor, resp);
      
      Map<QName, XdmValue> vars = new HashMap<QName, XdmValue>();
      staticContext = new RestXqStaticContext(processor, config, restXQuery, null, vars, registry);
      
      logger.info("Initialized RESTXQ static context");
    } catch (Exception e) {
        // TODO: is everything already logged to response and slf4j?
    }
  }
 
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (staticContext != null)
      initStaticContext(resp);
    
    Map<QName, XdmValue> vars = getDefaultExternalVariables(req, resp);
    HttpRequest request = new HttpServletRequestAdapter(req, req.getPathInfo());
    RestXqDynamicContext dynamicContext = new RestXqDynamicContext(staticContext, request, vars);
   
    doService(dynamicContext, req, resp);
  }
  
}