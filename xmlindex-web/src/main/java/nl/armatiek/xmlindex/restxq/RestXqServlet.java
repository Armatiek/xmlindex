package nl.armatiek.xmlindex.restxq;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.exquery.http.HttpRequest;
import org.exquery.restxq.RestXqService;
import org.exquery.restxq.RestXqServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.WebContext;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletRequestAdapter;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletResponseAdapter;

public class RestXqServlet extends HttpServlet {
  
  private static final long serialVersionUID = -1676378566658614933L;

  private static final Logger logger = LoggerFactory.getLogger(RestXqServlet.class);
  
  private File homeDir;
  private File indexesDir;
  private WebContext context;
  
  public void init() throws ServletException {    
    super.init();   
    try {                    
      context = WebContext.getInstance();
      homeDir = context.getHomeDir();    
      indexesDir = context.getIndexesDir();
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new ServletException(e);
    }
  }
  
  @Override
  public void destroy() {
    try {
      for (IndexInfo indexInfo : context.getIndexInfos()) {
        logger.info("Closing index \"" + indexInfo.getIndex().getIndexName() + "\" ... ");
        indexInfo.close();
      }
    } catch (Exception e) {
      logger.error("Error closing IndexInfo", e);
    } 
    super.destroy();
  }
  
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
          service.service(
              requestAdapter,
              new HttpServletResponseAdapter(resp),
              new ResourceFunctionExecutorImpl(index.getSaxonConfiguration(), indexInfo.getRestXQuery(), req.getContextPath() + req.getServletPath(), req.getRequestURI(), index.getIndexName()),
              new RestXqServiceSerializerImpl(index.getSaxonProcessor()));
        } else {
          if (logger.isTraceEnabled())
            logger.trace("Received " + requestAdapter.getMethod().name() + " request for \"" + requestAdapter.getPath() + "\" but no suitable Resource Function found");
          super.service(req, resp);
        }
      } finally {
        index.returnSession(session);
      }
    } catch (Exception e) {
      throw new ServletException(e); // TODO
    }
  }
  
}