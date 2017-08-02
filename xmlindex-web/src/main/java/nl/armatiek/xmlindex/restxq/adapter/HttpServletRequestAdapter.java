package nl.armatiek.xmlindex.restxq.adapter;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.exquery.http.HttpMethod;
import org.exquery.http.HttpRequest;

public class HttpServletRequestAdapter implements HttpRequest {

  private final HttpServletRequest request;
  private Map<String, List<String>> formFields = null;
  private final String path;

  public HttpServletRequestAdapter(final HttpServletRequest request, String path) {
    this.request = request;
    this.path = path;
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.valueOf(request.getMethod());
  }

  @Override
  public String getScheme() {
    return request.getScheme();
  }

  @Override
  public String getHostname() {
    return request.getServerName();
  }

  @Override
  public int getPort() {
    return request.getServerPort();
  }

  @Override
  public String getQuery() {
    return request.getQueryString();
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getURI() {
    return request.getRequestURI();
  }

  @Override
  public String getAddress() {
    return request.getLocalAddr();
  }

  @Override
  public String getRemoteHostname() {
    return request.getRemoteHost();
  }

  @Override
  public String getRemoteAddress() {
    return request.getRemoteAddr();
  }

  @Override
  public int getRemotePort() {
    return request.getRemotePort();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return request.getInputStream();
  }

  @Override
  public String getContentType() {
    return request.getContentType();
  }

  @Override
  public int getContentLength() {
    return request.getContentLength();
  }

  @Override
  public List<String> getHeaderNames() {
    final List<String> names = new ArrayList<>();
    for (final Enumeration<String> enumNames = request.getHeaderNames(); enumNames.hasMoreElements();) {
      names.add(enumNames.nextElement());
    }
    return names;
  }

  @Override
  public String getHeader(final String httpHeaderName) {
    return request.getHeader(httpHeaderName);
  }

  @Override
  public String getCookieValue(final String cookieName) {
    final Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (final Cookie cookie : cookies) {
        if (cookie.getName().equals(cookieName)) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  @Override
  public String getCharacterEncoding() {
    return request.getCharacterEncoding();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getFormParam(final String key) {
    if (request.getMethod().equals("GET")) {
      return getGetParameters(key);
    }
    if (request.getMethod().equals("POST") && request.getContentType() != null && request.getContentType().equals("application/x-www-form-urlencoded")) {
      if (formFields == null) {
        try {
          final InputStream in = getInputStream();
          formFields = extractFormFields(in);
        } catch (final IOException ioe) {
          // TODO log or something?
          ioe.printStackTrace();
          return null;
        }
      }
      final List<String> formFieldValues = formFields.get(key);
      if (formFieldValues != null) {
        return formFieldValues;
      } else {
        // fallback to get parameters
        return getGetParameters(key);
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getQueryParam(final String key) {
    return getGetParameters(key);
  }

  private Object getGetParameters(final String key) {
    final String[] values = request.getParameterValues(key);
    if (values != null) {
      if (values.length == 1) {
        return values[0];
      } else {
        return Arrays.asList(values);
      }
    }
    return null;
  }

  @Override
  public List<String> getParameterNames() {
    final List<String> names = new ArrayList<>();
    for (final Enumeration<String> enumNames = request.getParameterNames(); enumNames.hasMoreElements();) {
      names.add(enumNames.nextElement());
    }
    return names;
  }

  private Map<String, List<String>> extractFormFields(final InputStream in) throws IOException {
    final Map<String, List<String>> fields = new Hashtable<>();

    final StringBuilder builder = new StringBuilder();
    try (final Reader reader = new InputStreamReader(in)) {
      int read = -1;
      final char[] cbuf = new char[1024];
      while ((read = reader.read(cbuf)) > -1) {
        builder.append(cbuf, 0, read);
      }
    }

    final StringTokenizer st = new StringTokenizer(builder.toString(), "&");

    String key;
    String val;

    while (st.hasMoreTokens()) {
      final String pair = st.nextToken();
      final int pos = pair.indexOf('=');
      if (pos == -1) {
        throw new IllegalArgumentException();
      }

      try {
        key = java.net.URLDecoder.decode(pair.substring(0, pos), UTF_8.name());
        val = java.net.URLDecoder.decode(pair.substring(pos + 1, pair.length()), UTF_8.name());
      } catch (final Exception e) {
        throw new IllegalArgumentException(e);
      }

      List<String> vals = fields.get(key);
      if (vals == null) {
        vals = new ArrayList<>();
      }
      vals.add(val);

      fields.put(key, vals);
    }

    return fields;
  }
}