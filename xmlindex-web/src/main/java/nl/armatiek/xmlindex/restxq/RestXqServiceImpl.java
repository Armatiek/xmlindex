package nl.armatiek.xmlindex.restxq;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Part;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.exquery.http.HttpRequest;
import org.exquery.restxq.ResourceFunction;
import org.exquery.restxq.RestXqErrorCodes;
import org.exquery.restxq.RestXqServiceException;
import org.exquery.restxq.impl.AbstractRestXqService;
import org.exquery.xdm.type.SequenceImpl;
import org.exquery.xquery.Sequence;
import org.exquery.xquery.TypedValue;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Base64BinaryValue;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.restxq.adapter.HttpServletRequestAdapter;
import nl.armatiek.xmlindex.restxq.typedvalue.BinaryTypedValue;
import nl.armatiek.xmlindex.restxq.typedvalue.DocumentTypedValue;
import nl.armatiek.xmlindex.restxq.typedvalue.StringTypedValue;

public class RestXqServiceImpl extends AbstractRestXqService {

  private Configuration config;
  
  public RestXqServiceImpl(final ResourceFunction resourceFunction, Configuration config) {
    super(resourceFunction);
    this.config = config;
  }

  protected TypedValue<?> getSequence(InputStream is, String contentType) throws XPathException, IOException {
    if (StringUtils.contains(contentType, ";"))
      contentType = contentType.split(";")[0].trim();
    if (StringUtils.startsWithAny(contentType, "application/xml", "text/xml") || StringUtils.endsWith(contentType, "+xml")) {
      TreeInfo treeInfo = config.buildDocumentTree(new StreamSource(is));
      return new DocumentTypedValue(treeInfo.getRootNode());
    } else if (StringUtils.startsWith(contentType, "text/plain"))      
      return new StringTypedValue(new StringValue(IOUtils.toString(is, "UTF-8")));
    else
      return new BinaryTypedValue(new Base64BinaryValue(IOUtils.toByteArray(is)));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected Sequence extractRequestBody(final HttpRequest request) throws RestXqServiceException {
    try {
      String method = request.getMethod().name();
      boolean hasBody = method.equals("PUT") || (method.equals("POST") && !StringUtils.startsWith(request.getContentType(), "application/x-www-form-urlencoded"));
      if (!hasBody)
        return Sequence.EMPTY_SEQUENCE;
      
      if (StringUtils.startsWith(request.getContentType(), "multipart/")) {
        try {
          Collection<Part> parts = ((HttpServletRequestAdapter) request).getUnderlyingRequest().getParts();
          SequenceImpl seq = new SequenceImpl<>();
          List<String> paramNames = request.getParameterNames();
          for (Part part : parts)
            if (paramNames == null || !paramNames.contains(part.getName()))
              seq.add(getSequence(part.getInputStream(), part.getContentType()));
          return seq;
        } catch (ServletException se) {
          throw new RestXqServiceException("Error handling multipart request", se);
        }
      }
      return new SequenceImpl<>(getSequence(request.getInputStream(), request.getContentType()));
      
      /*
      PushbackInputStream pushbackStream = new PushbackInputStream(request.getInputStream());    
      int b = pushbackStream.read();
      if (b == -1) {
        return Sequence.EMPTY_SEQUENCE;
      }
      pushbackStream.unread(b);
      String contentType = request.getContentType();
      if (contentType != null && contentType.contains(";")) {
        contentType = contentType.split(";")[0].trim();
      }
      if ((contentType != null) && 
          (contentType.startsWith("text/xml") || contentType.startsWith("application/xml") ||
          contentType.endsWith("+xml"))) {
        TreeInfo treeInfo = config.buildDocumentTree(new StreamSource(pushbackStream));
        return new SequenceImpl<>(new DocumentTypedValue(treeInfo.getRootNode()));
      } else if ((contentType != null) && contentType.startsWith("text/plain")) {      
        return new SequenceImpl<>(new StringTypedValue(new StringValue(IOUtils.toString(pushbackStream, "UTF-8"))));
      } else {
        return new SequenceImpl<>(new BinaryTypedValue(new Base64BinaryValue(IOUtils.toByteArray(pushbackStream))));
      }
      */
      
    } catch (XPathException | IOException ioe) {
      throw new RestXqServiceException(RestXqErrorCodes.RQDY0014, ioe);
    }
    
  }

}