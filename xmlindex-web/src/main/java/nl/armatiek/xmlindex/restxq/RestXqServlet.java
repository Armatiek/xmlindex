package nl.armatiek.xmlindex.restxq;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
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
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.ObjectValue;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.WebDefinitions;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletRequestAdapter;
import nl.armatiek.xmlindex.saxon.conf.XMLIndexWebInitializer;

public class RestXqServlet extends AbstractRestXqServlet {
  
  private static final long serialVersionUID = -1676378566658614933L;

  private static final Logger logger = LoggerFactory.getLogger(RestXqServlet.class);
  
  private Map<String, RestXqStaticContext> staticContexts = new ConcurrentHashMap<String, RestXqStaticContext>();
  
  protected synchronized RestXqStaticContext getStaticContext(String indexName, HttpServletResponse resp) throws Exception {
    RestXqStaticContext staticContext = staticContexts.get(indexName);
    if (staticContext == null || developmentMode) {
      logger.info("Initializing RESTXQ static context for index \"" + indexName + "\" ...");
      
      XMLIndex index = context.getIndex(indexName);
      
      Configuration config = index.getSaxonConfiguration();
      new XMLIndexWebInitializer().initialize(config);
      Processor processor = index.getSaxonProcessor();
      
      RestXqServiceRegistryImpl registry = new RestXqServiceRegistryImpl();
      registry.addListener(new RestXqServiceRegistryLogger());
      
      File restXqFile = new File(context.getIndexesDir(), indexName + File.separatorChar + WebDefinitions.FILENAME_RESTXQ);
      if (!restXqFile.isFile()) {
        logger.info("Creating new RestXQ module: \"" + WebDefinitions.FILENAME_RESTXQ + "\" ...");
        File restXqDir = restXqFile.getParentFile();
        if (!restXqDir.exists() && !restXqDir.mkdirs())
          throw new XMLIndexException("Could not create directory \"" + restXqFile.getParentFile().getAbsolutePath() + "\"");
        InputStream in = getClass().getClassLoader().getResourceAsStream(WebDefinitions.FILENAME_RESTXQ.replace('\\', '/')); 
        FileUtils.copyInputStreamToFile(in, restXqFile);
      }
    
      XQueryExecutable restXQuery = compileAndRegisterRestXQuery(restXqFile, registry, processor, resp);
      
      Map<QName, XdmValue> vars = new HashMap<QName, XdmValue>();
      staticContext = new RestXqStaticContext(processor, config, restXQuery, vars, registry);
      
      logger.info("Initialized RESTXQ static context for index \"" + indexName + "\"");
      
      staticContexts.put(indexName, staticContext);
    }
    return staticContext;
  }
  
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    /* Get name of index: */
    String pathInfo = req.getPathInfo();
    if (StringUtils.equalsAny(pathInfo, "", "/")) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Index name not specified in path");
      return;
    }
    String indexName = req.getPathInfo().split("/")[1];
    
    /* Get static context: */
    RestXqStaticContext staticContext;
    try {
      staticContext = getStaticContext(indexName, resp);
    } catch (Exception e) {
      handleError("Error initializing RESTXQ static context for index \"" + indexName + "\"", e, resp);
      return;
    }
    
    /* Get session: */
    XMLIndex index = context.getIndex(indexName);
    Session session = index.aquireSession();
   
    /* Get dynamic context: */
    try {
      RestXqDynamicContext dynamicContext;
      try {
        Map<QName, XdmValue> vars = getDefaultExternalVariables(req, resp);
        boolean developmentMode = index.getConfiguration().getDevelopmentMode();
        vars.put(XI_DEV_MODE, XdmValue.wrap(BooleanValue.get(developmentMode)));
        if (session != null)
          vars.put(Definitions.PARAM_SESSION_QN, XdmValue.wrap(new ObjectValue<Session>(session)));
        String path = pathInfo.substring(indexName.length() + 1);
        path = path.length() == 0 ? "/" : path;
        HttpRequest request = new HttpServletRequestAdapter(req, path);
        dynamicContext = new RestXqDynamicContext(staticContext, request, vars, developmentMode);
      } catch (Exception e) {
        handleError("Error initializing RESTXQ dynamic context", e, resp);
        return;
      }
      
      doService(dynamicContext, req, resp);
      
    } finally {
      index.returnSession(session);
    }
  }
  
}