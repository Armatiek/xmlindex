package nl.armatiek.xmlindex.restxq;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.exquery.http.HttpRequest;
import org.exquery.restxq.impl.RestXqServiceRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.value.ObjectValue;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletRequestAdapter;
import nl.armatiek.xmlindex.saxon.conf.XMLIndexWebInitializer;

@MultipartConfig
public class IDERestXqServlet extends AbstractRestXqServlet {

  private static final long serialVersionUID = 1135079367879721547L;

  private static final Logger logger = LoggerFactory.getLogger(IDERestXqServlet.class);
  
  private RestXqStaticContext staticContext;
  
  protected synchronized void initStaticContext(HttpServletResponse resp) throws Exception {
    if (!developmentMode && staticContext != null)
      return;
    
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
    
  }
  
  protected void handleError(String msg, Throwable t, HttpServletResponse resp) {
    try {
      logger.error(msg, t);
      if (developmentMode) {
        IOUtils.copy(new StringReader("\n" + msg + "\n\n"), resp.getOutputStream(), StandardCharsets.UTF_8);
        t.printStackTrace(new PrintStream(resp.getOutputStream()));
      } else   
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
    } catch (IOException ioe) {
      logger.error("Error handling error", ioe);
    }
  }
 
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (developmentMode || staticContext == null)
      try {
        initStaticContext(resp);
      } catch (Exception e) {
        handleError("Error initializing RESTXQ static context", e, resp);
        return;
      }
    
    XMLIndex index = null;
    Session session = null;
    String indexName = req.getParameter("index");
    if (!StringUtils.isBlank(indexName)) {
      try {
        index = context.getIndex(indexName);
        session = index.aquireSession();
      } catch (IOException ioe) {
        handleError("Error acquiring session for index \"" + indexName + "\"", ioe, resp);
        return;
      }
    }
    
    try {
      RestXqDynamicContext dynamicContext;
      try {
        Map<QName, XdmValue> vars = getDefaultExternalVariables(req, resp);
        if (session != null)
          vars.put(Definitions.PARAM_SESSION_QN, XdmValue.wrap(new ObjectValue<Session>(session)));
        HttpRequest request = new HttpServletRequestAdapter(req, req.getPathInfo());
        dynamicContext = new RestXqDynamicContext(staticContext, request, vars);
      } catch (Exception e) {
        handleError("Error initializing RESTXQ dynamic context", e, resp);
        return;
      }
      
      doService(dynamicContext, req, resp);
      
    } finally {
      if (index != null && session != null)
        index.returnSession(session);
    }
  }
  
}