package nl.armatiek.xmlindex.restxq;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.exquery.ExQueryException;
import org.exquery.restxq.ResourceFunction;
import org.exquery.restxq.RestXqServiceRegistry;
import org.exquery.restxq.impl.ResourceFunctionFactory;
import org.exquery.restxq.impl.RestXqServiceRegistryImpl;
import org.exquery.xquery3.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.WebDefinitions;
import nl.armatiek.xmlindex.restxq.adapter.AnnotationAdapter;

public final class RestXqServiceRegistryManager {

  private static final Logger logger = LoggerFactory.getLogger(RestXqServiceRegistryManager.class);

  private RestXqServiceRegistryImpl registry = null;
  private XQueryExecutable restXQuery;
 
  public synchronized RestXqServiceRegistry getRegistry(XMLIndex index) 
      throws IOException, ExQueryException, SaxonApiException {
    try {
      if (registry == null) {
        logger.info("Initializing RESTXQ...");
        registry = new RestXqServiceRegistryImpl();
        registry.addListener(new RestXqServiceRegistryLogger());
  
        // add compiled cache cleanup listener
        // registry.addListener(new RestXqServiceCompiledXQueryCacheCleanupListener());
  
        File restXQFile = new File(index.getIndexPath().toFile(), WebDefinitions.FILENAME_RESTXQ);
        if (!restXQFile.isFile()) {
          logger.info("Creating \"" + WebDefinitions.FILENAME_RESTXQ + "\"");
          // TODO
        }
        XQueryCompiler comp = index.getSaxonProcessor().newXQueryCompiler();
        comp.declareNamespace("rest", WebDefinitions.NAMESPACE_RESTXQ);
        comp.declareNamespace("output", WebDefinitions.NAMESPACE_OUTPUT);
        //if (errorListener != null)
        //  comp.setErrorListener(errorListener);
        this.restXQuery = comp.compile(restXQFile);
        Iterator<XQueryFunction> iter = restXQuery.getUnderlyingCompiledQuery().getMainModule().getGlobalFunctionLibrary().getFunctionDefinitions();
        while (iter.hasNext()) {
          XQueryFunction func = iter.next();
          Set<Annotation> annotations = new HashSet<Annotation>();
          for (net.sf.saxon.query.Annotation an : func.getAnnotations())
            annotations.add(new AnnotationAdapter(an, func));
          ResourceFunction resourceFunction = ResourceFunctionFactory.create(restXQFile.toURI(), annotations);
          registry.register(new RestXqServiceImpl(resourceFunction, index.getSaxonConfiguration()));
        }
        logger.info("RESTXQ is ready.");
      }
    } catch (Exception e) {
      registry = null;
    }

    return registry;
  }
  
  public XQueryExecutable getRestXQuery() {
    return restXQuery;
  }
}