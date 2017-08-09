package nl.armatiek.xmlindex.web.servlet;

import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import nl.armatiek.xmlindex.conf.WebContext;

public class StaticServlet extends FileServlet {

  private static final long serialVersionUID = 9044514244691816893L;
  
  private static final String PREFIX_IDE = "ide/";
  
  private File indexesDir;
  private File ideDir;
  
  @Override
  public void init() throws ServletException {
    this.indexesDir = WebContext.getInstance().getIndexesDir();
    this.ideDir = new File(WebContext.getInstance().getHomeDir(), "ide");
  }

  @Override
  protected File getFile(HttpServletRequest request) {
    String path = request.getParameter("path");
    path = StringUtils.removeStart(path, request.getContextPath() + "/");
    if (path.startsWith(PREFIX_IDE))
      return new File(ideDir, path.substring(PREFIX_IDE.length()));
    return new File(indexesDir, path);
  }

}
