package nl.armatiek.xmlindex.saxon.functionset;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.registry.BuiltInFunctionSet;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ZeroOrMore;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.conf.WebDefinitions;

public class RequestFunctionSet extends BuiltInFunctionSet {

  private static RequestFunctionSet THE_INSTANCE = new RequestFunctionSet();
  
  private static final StructuredQName PARAM_NAME_REQUEST = new StructuredQName("", WebDefinitions.NAMESPACE_REQUEST, "request");

  public static RequestFunctionSet getInstance() {
    return THE_INSTANCE;
  }

  private RequestFunctionSet() {
    init();
  }

  private void init() {
    register("method", 0, MethodFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("scheme", 0, SchemeFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("hostname", 0, HostNameFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("port", 0, PortFn.class, BuiltInAtomicType.INTEGER, ONE, 0, LATE);
    register("path", 0, PathFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("context-path", 0, ContextPathFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("query", 0, QueryFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("uri", 0, UriFn.class, BuiltInAtomicType.ANY_URI, ONE, 0, LATE);
    register("address", 0, AddressFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("remote-hostname", 0, RemoteHostNameFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("remote-address", 0, RemoteAddressFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE);
    register("remote-port", 0, RemotePortFn.class, BuiltInAtomicType.INTEGER, ONE, 0, LATE);
    register("parameter-names", 0, ParameterNamesFn.class, BuiltInAtomicType.STRING, STAR, 0, LATE);
    register("parameter", 1, ParameterFn.class, BuiltInAtomicType.STRING, OPT, 0, LATE).arg(0, BuiltInAtomicType.STRING, ONE, EMPTY);
    register("parameter", 2, ParameterFn.class, BuiltInAtomicType.STRING, OPT, 0, LATE).arg(0, BuiltInAtomicType.STRING, ONE, EMPTY).arg(1, BuiltInAtomicType.STRING, STAR, null);
    register("header-names", 0, HeaderNamesFn.class, BuiltInAtomicType.STRING, PLUS, 0, LATE);
    register("header", 1, HeaderFn.class, BuiltInAtomicType.STRING, OPT, 0, LATE).arg(0, BuiltInAtomicType.STRING, ONE, EMPTY);
    register("header", 2, HeaderFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE).arg(0, BuiltInAtomicType.STRING, ONE, EMPTY).arg(1, BuiltInAtomicType.STRING, ONE, null);
    register("cookie-names", 0, CookieNamesFn.class, BuiltInAtomicType.STRING, STAR, 0, LATE);
    register("cookie", 1, CookieFn.class, BuiltInAtomicType.STRING, OPT, 0, LATE).arg(0, BuiltInAtomicType.STRING, ONE, EMPTY);
    register("cookie", 2, CookieFn.class, BuiltInAtomicType.STRING, ONE, 0, LATE).arg(0, BuiltInAtomicType.STRING, ONE, EMPTY).arg(1, BuiltInAtomicType.STRING, ONE, null); 
    register("attribute", 1, AttributeFn.class, BuiltInAtomicType.STRING, OPT, 0, LATE).arg(0, BuiltInAtomicType.STRING, ONE, EMPTY);
  }

  @Override
  public String getNamespace() {
    return WebDefinitions.NAMESPACE_REQUEST;
  }

  @Override
  public String getConventionalPrefix() {
    return "request";
  }
  
  public static class MethodFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getMethod());
    }
  }
  
  public static class SchemeFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getScheme());
    }
  }
  
  public static class HostNameFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getServerName());
    }
  }
  
  public static class PortFn extends SystemFunction {
    public Int64Value call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new Int64Value(getRequest(context).getServerPort());
    }
  }
  
  public static class PathFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getRequestURI());
    }
  }
  
  public static class ContextPathFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getContextPath());
    }
  }
  
  public static class QueryFn extends SystemFunction {
    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      final String query = getRequest(context).getQueryString();
      return (query == null) ? ZeroOrOne.empty() : new ZeroOrOne<StringValue>(new StringValue(query));
    }
  }
  
  public static class UriFn extends SystemFunction {
    public AnyURIValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new AnyURIValue(getRequest(context).getRequestURL().toString());
    }
  }
  
  public static class AddressFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getLocalAddr());
    }
  }
  
  public static class RemoteHostNameFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getRemoteHost());
    }
  }
  
  public static class RemoteAddressFn extends SystemFunction {
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new StringValue(getRequest(context).getRemoteAddr());
    }
  }
  
  public static class RemotePortFn extends SystemFunction {
    public Int64Value call(XPathContext context, Sequence[] arguments) throws XPathException {
      return new Int64Value(getRequest(context).getRemotePort());
    }
  }
  
  public static class ParameterNamesFn extends SystemFunction {
    public ZeroOrMore<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      List<StringValue> valueList = new ArrayList<StringValue>();
      Enumeration<String> params = getRequest(context).getParameterNames();
      while (params.hasMoreElements())
        valueList.add(new StringValue(params.nextElement()));
      return new ZeroOrMore<StringValue>(valueList);
    }
  }
  
  public static class ParameterFn extends SystemFunction {
    public ZeroOrMore<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      HttpServletRequest request = getRequest(context);
      List<StringValue> valueList = new ArrayList<StringValue>();
      StringValue paramName = (StringValue) arguments[0].head();
      if (request.getParameterMap().keySet().contains(paramName)) {
        String[] values = request.getParameterValues(paramName.getStringValue());
        for (String value : values)
          valueList.add(new StringValue(value));
      } else if (arguments.length == 2) {
        StringValue defaultValue;
        SequenceIterator iter = arguments[1].iterate();
        while ((defaultValue = (StringValue) iter.next()) != null)
          valueList.add(defaultValue);
      }
      return new ZeroOrMore<StringValue>(valueList);
    }
  }
  
  public static class HeaderNamesFn extends SystemFunction {
    public ZeroOrMore<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      List<StringValue> nameList = new ArrayList<StringValue>();
      Enumeration<String> headers = getRequest(context).getHeaderNames();
      while (headers.hasMoreElements())
        nameList.add(new StringValue(headers.nextElement()));
      return new ZeroOrMore<StringValue>(nameList);
    }
  }
  
  public static class HeaderFn extends SystemFunction {
    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      StringValue headerName = (StringValue) arguments[0].head();
      String headerValue = getRequest(context).getHeader(headerName.getStringValue());
      if (headerValue != null)
        return new ZeroOrOne<StringValue>(new StringValue(headerValue));
      if (arguments.length == 2)
        return new ZeroOrOne<StringValue>((StringValue) arguments[1].head());
      return ZeroOrOne.empty();
    }
  }
  
  public static class CookieNamesFn extends SystemFunction {
    public ZeroOrMore<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      Cookie[] cookies = getRequest(context).getCookies();
      List<StringValue> nameList = new ArrayList<StringValue>();
      for (Cookie cookie : cookies)
        nameList.add(new StringValue(cookie.getName()));
      return new ZeroOrMore<StringValue>(nameList);
    }
  }
  
  public static class CookieFn extends SystemFunction {
    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      StringValue cookieName = (StringValue) arguments[0].head();
      Cookie[] cookies = getRequest(context).getCookies();
      for (Cookie cookie : cookies)
        if (cookie.getName().equals(cookieName))
          return new ZeroOrOne<StringValue>(new StringValue(cookie.getValue()));
      if (arguments.length == 2)
        return new ZeroOrOne<StringValue>(((StringValue) arguments[1].head()));
      return ZeroOrOne.empty();
    }
  }
  
  public static class AttributeFn extends SystemFunction {
    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
      StringValue attrName = (StringValue) arguments[0].head();
      String attrVal = (String) getRequest(context).getAttribute(attrName.getStringValue());
      return attrVal != null ? new ZeroOrOne<StringValue>(new StringValue(attrVal)) : ZeroOrOne.empty();
    }
  }
  
  private static HttpServletRequest getRequest(XPathContext context) {
    return (HttpServletRequest) ((ObjectValue<?>) context.getController().getParameter(PARAM_NAME_REQUEST)).getObject();
  }
  
}