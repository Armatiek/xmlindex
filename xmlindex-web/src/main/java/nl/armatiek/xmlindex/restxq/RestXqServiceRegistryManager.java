package nl.armatiek.xmlindex.restxq;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.exquery.ExQueryException;
import org.exquery.restxq.ResourceFunction;
import org.exquery.restxq.RestXqServiceRegistry;
import org.exquery.restxq.impl.ResourceFunctionFactory;
import org.exquery.restxq.impl.RestXqServiceRegistryImpl;
import org.exquery.restxq.impl.annotation.RestAnnotationFactory;
import org.exquery.xquery3.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.WebDefinitions;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.restxq.adapter.AnnotationAdapter;

public final class RestXqServiceRegistryManager {

  private static final Logger logger = LoggerFactory.getLogger(RestXqServiceRegistryManager.class);

  private RestXqServiceRegistryImpl registry = null;
  private XQueryExecutable restXQuery;
 
  public synchronized RestXqServiceRegistry getRegistry(XMLIndex index) 
      throws IOException, ExQueryException, SaxonApiException {
    try {
      if (true || registry == null) {
        logger.info("Initializing RESTXQ...");
        registry = new RestXqServiceRegistryImpl();
        registry.addListener(new RestXqServiceRegistryLogger());
  
        // add compiled cache cleanup listener
        // registry.addListener(new RestXqServiceCompiledXQueryCacheCleanupListener());
  
        File restXqFile = new File(index.getIndexPath().toFile(), WebDefinitions.FILENAME_RESTXQ);
        if (!restXqFile.isFile()) {
          logger.info("Creating RestXQ module: \"" + WebDefinitions.FILENAME_RESTXQ + "\" ...");
          File restXqDir = restXqFile.getParentFile();
          if (!restXqDir.exists() && !restXqDir.mkdirs())
            throw new XMLIndexException("Could not create directory \"" + restXqFile.getParentFile().getAbsolutePath() + "\"");
          InputStream in = getClass().getClassLoader().getResourceAsStream(WebDefinitions.FILENAME_RESTXQ.replace('\\', '/')); 
          FileUtils.copyInputStreamToFile(in, restXqFile);
        }
        XQueryCompiler comp = index.getSaxonProcessor().newXQueryCompiler();
        comp.declareNamespace("rest", WebDefinitions.NAMESPACE_RESTXQ);
        comp.declareNamespace("output", WebDefinitions.NAMESPACE_OUTPUT);
        //if (errorListener != null)
        //  comp.setErrorListener(errorListener);
        
        this.restXQuery = comp.compile(restXqFile);
        Iterator<XQueryFunction> iter = restXQuery.getUnderlyingCompiledQuery().getMainModule().getGlobalFunctionLibrary().getFunctionDefinitions();
        while (iter.hasNext()) {
          XQueryFunction func = iter.next();
          Set<Annotation> annotations = new HashSet<Annotation>();
          for (net.sf.saxon.query.Annotation an : func.getAnnotations()) {
            if (RestAnnotationFactory.isRestXqAnnotation(an.getAnnotationQName().toJaxpQName())) {
              final org.exquery.xquery3.Annotation restAnnotation = RestAnnotationFactory.getAnnotation(new AnnotationAdapter(an, func));
              annotations.add(restAnnotation);
            }
          }
          if (!annotations.isEmpty()) {
            ResourceFunction resourceFunction = ResourceFunctionFactory.create(restXqFile.toURI(), annotations);
            registry.register(new RestXqServiceImpl(resourceFunction, index.getSaxonConfiguration()));
          }
        }
        logger.info("RESTXQ is ready.");
      }
    } catch (Exception e) {
      registry = null;
      throw e;
    }

    return registry;
  }
  
  public XQueryExecutable getRestXQuery() {
    return restXQuery;
  }
}