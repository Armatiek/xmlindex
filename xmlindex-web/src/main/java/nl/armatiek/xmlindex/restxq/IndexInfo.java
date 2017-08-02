package nl.armatiek.xmlindex.restxq;

import java.io.File;
import java.io.IOException;

import org.exquery.ExQueryException;
import org.exquery.restxq.RestXqServiceRegistry;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryExecutable;
import nl.armatiek.xmlindex.XMLIndex;

public class IndexInfo {
  
  // private static final Logger logger = LoggerFactory.getLogger(IndexInfo2.class);
  
  private XMLIndex index;
  private RestXqServiceRegistryManager serviceRegistryManager;
  
  public IndexInfo(File indexDir) throws IOException {
    this.index = new XMLIndex(indexDir.getName(), indexDir.toPath());
    this.serviceRegistryManager = new RestXqServiceRegistryManager();
  }
  
  public RestXqServiceRegistry getServiceRegistryManager() throws IOException, ExQueryException, SaxonApiException {
    return serviceRegistryManager.getRegistry(this.index);
  }
  
  public void close() throws IOException {
    if (index.isOpen())
      index.close();
  }
  
  public XMLIndex getIndex() {
    return index;
  }
  
  public XQueryExecutable getRestXQuery() {
    return serviceRegistryManager.getRestXQuery();
  }
  
}