package nl.armatiek.xmlindex.restxq.adapter;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import org.exquery.http.HttpResponse;
import org.exquery.http.HttpStatus;

public class HttpServletResponseAdapter implements HttpResponse {

  private final HttpServletResponse response;

  public HttpServletResponseAdapter(final HttpServletResponse response) {
    this.response = response;
  }

  @Override
  public void setHeader(final String name, final String value) {
    response.setHeader(name, value);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setStatus(final HttpStatus status, final String reason) {
    response.setStatus(status.getStatus(), reason);
  }

  @Override
  public void setStatus(final HttpStatus status) {
    response.setStatus(status.getStatus());
  }

  @Override
  public boolean containsHeader(final String name) {
    return response.containsHeader(name);
  }

  @Override
  public void setContentType(final String contentType) {
    response.setContentType(contentType);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return response.getOutputStream();
  }

  @Override
  public boolean isCommitted() {
    return response.isCommitted();
  }
}