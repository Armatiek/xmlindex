package nl.armatiek.xmlindex.restxq;

import java.io.IOException;
import java.io.PushbackInputStream;

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

import net.sf.saxon.Configuration;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Base64BinaryValue;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.restxq.typedvalue.BinaryTypedValue;
import nl.armatiek.xmlindex.restxq.typedvalue.DocumentTypedValue;
import nl.armatiek.xmlindex.restxq.typedvalue.StringTypedValue;

public class RestXqServiceImpl extends AbstractRestXqService {

  private Configuration config;
  
  public RestXqServiceImpl(final ResourceFunction resourceFunction, Configuration config) {
    super(resourceFunction);
    this.config = config;
  }

  /*
  @Override
  public void service(final HttpRequest request, final HttpResponse response, 
      final ResourceFunctionExecuter resourceFunctionExecuter, 
      final RestXqServiceSerializer restXqServiceSerializer) throws RestXqServiceException {
    super.service(request, response, resourceFunctionExecuter, restXqServiceSerializer);
  }
  */

  @SuppressWarnings("rawtypes")
  @Override
  protected Sequence extractRequestBody(final HttpRequest request) throws RestXqServiceException {
    try {
      String method = request.getMethod().name();
      if (!StringUtils.equalsAny(method, "GET", "POST") || StringUtils.startsWith(request.getContentType(), "application/x-www-form-urlencoded")) {
        return Sequence.EMPTY_SEQUENCE;
      }    
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
      // TODO: use mime table?
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
    } catch (XPathException | IOException ioe) {
      throw new RestXqServiceException(RestXqErrorCodes.RQDY0014, ioe);
    }
    
  }

}