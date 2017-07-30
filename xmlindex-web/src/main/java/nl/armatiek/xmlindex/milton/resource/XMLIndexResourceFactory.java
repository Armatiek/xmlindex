package nl.armatiek.xmlindex.milton.resource;

import java.io.IOException;

import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class XMLIndexResourceFactory implements ResourceFactory {

  private SecurityManager securityManager;
  
  private String contextPath;
  private XMLIndex index;
  
  public XMLIndexResourceFactory(String contextPath) {
    this.contextPath = contextPath;
  }
  
  @Override
  public Resource getResource(String host, String path) throws NotAuthorizedException, BadRequestException {
    try {
      Session session = index.aquireSession();
      try {
      
        
      } finally {
        index.returnSession(session);
      }
    } catch (IOException ioe) {
      throw new XMLIndexException("Error getting WebDAV resource for host \"" + host + "\" and path \"" + path + "\"");
    }
  }
  
  private String stripContext(String url) {
    if (contextPath != null && contextPath.length() > 0)
      return url.replaceFirst(contextPath, "");
    return url;
  }
  
}