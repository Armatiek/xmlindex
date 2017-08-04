package nl.armatiek.xmlindex.web.servlet;

import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import nl.armatiek.xmlindex.conf.WebContext;

public class StaticServlet extends FileServlet {

  private static final long serialVersionUID = 9044514244691816893L;
  
  private File indexesDir;
  
  @Override
  public void init() throws ServletException {
    this.indexesDir = WebContext.getInstance().getIndexesDir();
  }

  @Override
  protected File getFile(HttpServletRequest request) {
    String path = request.getParameter("path");
    path = StringUtils.removeStart(path, request.getContextPath() + "/");
    return new File(indexesDir, path);
  }

}
