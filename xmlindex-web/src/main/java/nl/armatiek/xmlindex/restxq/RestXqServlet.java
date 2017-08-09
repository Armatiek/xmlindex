package nl.armatiek.xmlindex.restxq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.exquery.http.HttpRequest;
import org.exquery.restxq.RestXqService;
import org.exquery.restxq.RestXqServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.WebContext;
import nl.armatiek.xmlindex.conf.WebDefinitions;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletRequestAdapter;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletResponseAdapter;

public class RestXqServlet extends HttpServlet {
  
  private static final long serialVersionUID = -1676378566658614933L;

  private static final Logger logger = LoggerFactory.getLogger(RestXqServlet.class);
  
  public final static QName XQ_VAR_BASE_URI  = new QName(WebDefinitions.NAMESPACE_RESTXQ, "base-uri");
  public final static QName XQ_VAR_URI       = new QName(WebDefinitions.NAMESPACE_RESTXQ, "uri");
  public final static QName XQ_VAR_INDEXNAME = new QName(WebDefinitions.NAMESPACE_RESTXQ, "index-name");
  
  private File indexesDir;
  private WebContext context;
  private boolean developmentMode;
  
  public void init() throws ServletException {    
    super.init();   
    try {                    
      context = WebContext.getInstance();
      indexesDir = context.getIndexesDir();
      developmentMode = context.getDevelopmentMode();
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new ServletException(e);
    }
  }
  
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    OutputStream respOs = resp.getOutputStream();
    
    try {  
      String pathInfo = req.getPathInfo();
      if (StringUtils.equalsAny(pathInfo, "", "/")) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Index name not specified in path");
        return;
      }
      String indexName = req.getPathInfo().split("/")[1];
      IndexInfo indexInfo = context.getIndexInfo(indexName.toLowerCase());
      if (indexInfo == null) {
        // TODO: concurrency:
        File indexDir = new File(indexesDir, indexName);
        if (!indexDir.isDirectory()) {
          resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Index \"" + indexName + "\" not found");
          return;
        }
        indexInfo = new IndexInfo(indexDir);
        context.addIndexInfo(indexDir.getName().toLowerCase(), indexInfo);
      }
      XMLIndex index = indexInfo.getIndex();
      Session session = index.aquireSession();
      try {
        String path = pathInfo.substring(indexName.length() + 1);
        path = path.length() == 0 ? "/" : path;
        final HttpRequest requestAdapter = new HttpServletRequestAdapter(req, path);

        RestXqServiceRegistry registry = indexInfo.getServiceRegistryManager();
        RestXqService service = registry.findService(requestAdapter);
        if (service != null) {
          if (logger.isTraceEnabled())
            logger.trace("Received " + requestAdapter.getMethod().name() + " request for \"" + requestAdapter.getPath() + "\" and found Resource Function \"" + service.getResourceFunction().getFunctionSignature() + "\" in  module \"" + service.getResourceFunction().getXQueryLocation() + "\"");
          
          OutputStream os = (developmentMode) ? new ByteArrayOutputStream() : respOs;
          
          Map<QName, XdmValue> params = new HashMap<QName, XdmValue>();
          params.put(XQ_VAR_BASE_URI, new XdmAtomicValue(req.getContextPath() + req.getServletPath()));
          params.put(XQ_VAR_URI, new XdmAtomicValue(req.getRequestURI()));
          params.put(XQ_VAR_INDEXNAME, new XdmAtomicValue(indexName));
          
          service.service(
              requestAdapter,
              new HttpServletResponseAdapter(resp, os),
              new ResourceFunctionExecutorImpl(indexInfo.getRestXQuery(), params, new XdmNode(session.getTreeInfo().getRootNode()), 
                  session.getConfiguration(), resp),
              new RestXqServiceSerializerImpl(index.getSaxonProcessor()));
          
          if (developmentMode) {     
            byte[] body = ((ByteArrayOutputStream) os).toByteArray();                         
            IOUtils.copy(new ByteArrayInputStream(body), respOs);
          }
          
        } else {
          if (logger.isTraceEnabled())
            logger.trace("Received " + requestAdapter.getMethod().name() + " request for \"" + requestAdapter.getPath() + "\" but no suitable Resource Function found");
          resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No function found that matches the request");
        }
      } finally {
        index.returnSession(session);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      if (developmentMode) {              
        resp.setContentType("text/plain; charset=UTF-8");        
        e.printStackTrace(new PrintStream(respOs));        
      } else if (!resp.isCommitted()) {
        resp.resetBuffer();
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.setContentType("text/html; charset=UTF-8");
        Writer w = new OutputStreamWriter(respOs, "UTF-8");
        w.write("<html><body><h1>Internal Server Error</h1></body></html>");
      }
    }
  }
  
}