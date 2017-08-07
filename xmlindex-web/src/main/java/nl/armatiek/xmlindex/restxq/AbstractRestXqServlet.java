package nl.armatiek.xmlindex.restxq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.exquery.ExQueryException;
import org.exquery.http.HttpRequest;
import org.exquery.restxq.ResourceFunction;
import org.exquery.restxq.RestXqService;
import org.exquery.restxq.RestXqServiceRegistry;
import org.exquery.restxq.impl.ResourceFunctionFactory;
import org.exquery.restxq.impl.annotation.RestAnnotationFactory;
import org.exquery.xquery3.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.value.ObjectValue;
import nl.armatiek.xmlindex.conf.WebContext;
import nl.armatiek.xmlindex.conf.WebDefinitions;
import nl.armatiek.xmlindex.restxq.adapter.AnnotationAdapter;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletResponseAdapter;
import nl.armatiek.xmlindex.saxon.TransformationErrorListener;

public abstract class AbstractRestXqServlet extends HttpServlet {
  
  private static final long serialVersionUID = -3141190667159953975L;

  private static final Logger logger = LoggerFactory.getLogger(AbstractRestXqServlet.class);
  
  public final static QName XQ_VAR_BASE_URI  = new QName(WebDefinitions.NAMESPACE_RESTXQ, "base-uri");
  public final static QName XQ_VAR_URI       = new QName(WebDefinitions.NAMESPACE_RESTXQ, "uri");
  public final static QName XQ_VAR_INDEXNAME = new QName(WebDefinitions.NAMESPACE_RESTXQ, "index-name");
  public final static QName XI_VAR_REQUEST   = new QName(WebDefinitions.NAMESPACE_REQUEST, "request");
  public final static QName XI_VAR_RESPONSE  = new QName(WebDefinitions.NAMESPACE_RESPONSE, "response");
  
  protected WebContext context;
  protected boolean developmentMode;
  
  @Override
  public void init() throws ServletException {    
    super.init();                    
    context = WebContext.getInstance();
    developmentMode = context.getDevelopmentMode();
  }
  
  protected void doService(RestXqDynamicContext dynamicContext, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    OutputStream respOs = resp.getOutputStream();
    try {  
      final RestXqStaticContext staticContext = dynamicContext.getStaticContext();
      final HttpRequest request = dynamicContext.getRequest();
      final RestXqServiceRegistry registry = staticContext.getServiceRegistry();
      final RestXqService service = registry.findService(request);
      if (service != null) {
        if (logger.isTraceEnabled())
          logger.trace("Received " + request.getMethod().name() + " request for \"" + request.getPath() + "\" and found Resource Function \"" + service.getResourceFunction().getFunctionSignature() + "\" in  module \"" + service.getResourceFunction().getXQueryLocation() + "\"");
        
        OutputStream os = (developmentMode) ? new ByteArrayOutputStream() : respOs;
        
        service.service(
            request,
            new HttpServletResponseAdapter(resp, os),
            new ResourceFunctionExecutorImpl(staticContext.getRestXQuery(), dynamicContext.getAllExternalVariables(), 
                staticContext.getContextItem(), staticContext.getConfiguration(), resp),
            new RestXqServiceSerializerImpl(staticContext.getProcessor()));
        
        if (developmentMode) {     
          byte[] body = ((ByteArrayOutputStream) os).toByteArray();                         
          IOUtils.copy(new ByteArrayInputStream(body), respOs);
        }
        
      } else {
        if (logger.isTraceEnabled())
          logger.trace("Received " + request.getMethod().name() + " request for \"" + request.getPath() + "\" but no suitable Resource Function found");
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No function found that matches the request");
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
  
  protected XQueryExecutable compileAndRegisterRestXQuery(File restXqFile, RestXqServiceRegistry registry, 
      Processor processor, HttpServletResponse response) 
      throws SaxonApiException, IOException, ExQueryException {
    if (!restXqFile.isFile())
      throw new FileNotFoundException("RESTXQ module \"" + restXqFile + "\" not found");
      
    XQueryCompiler comp = processor.newXQueryCompiler();
    comp.declareNamespace("rest", WebDefinitions.NAMESPACE_RESTXQ);
    comp.declareNamespace("output", WebDefinitions.NAMESPACE_OUTPUT);
    comp.setErrorListener(new TransformationErrorListener(response, developmentMode));
    
    XQueryExecutable restXQuery = comp.compile(restXqFile);
    
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
        registry.register(new RestXqServiceImpl(resourceFunction, processor.getUnderlyingConfiguration()));
      }
    }
    
    return restXQuery;  
  }
  
  protected Map<QName, XdmValue> getDefaultExternalVariables(HttpServletRequest req, HttpServletResponse resp) {
    Map<QName, XdmValue> vars = new HashMap<QName, XdmValue>();
    vars.put(XQ_VAR_BASE_URI, new XdmAtomicValue(req.getContextPath() + req.getServletPath()));
    vars.put(XQ_VAR_URI, new XdmAtomicValue(req.getRequestURI()));
    vars.put(XI_VAR_REQUEST,  XdmValue.wrap(new ObjectValue<HttpServletRequest>(req)));
    vars.put(XI_VAR_RESPONSE,  XdmValue.wrap(new ObjectValue<HttpServletResponse>(resp)));
    return vars;
  }
  
}