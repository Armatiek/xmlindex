package nl.armatiek.xmlindex.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import nl.armatiek.xmlindex.utils.XMLIndexWebUtils;

public class StaticUrlRewriteFilter implements Filter {

  @Override
  public void init(FilterConfig config) throws ServletException { }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
    HttpServletRequest request = (HttpServletRequest) req;
    String requestURI = request.getRequestURI();
    if (requestURI.contains("static/"))
      req.getRequestDispatcher("/static?path=" + XMLIndexWebUtils.encodeForURI(requestURI)).forward(req, res);
    else
      chain.doFilter(req, res);
  }

  @Override
  public void destroy() { }
  
}