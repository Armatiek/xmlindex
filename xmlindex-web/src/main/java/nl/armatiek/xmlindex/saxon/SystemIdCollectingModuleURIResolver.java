package nl.armatiek.xmlindex.saxon;

import java.util.List;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.lib.StandardModuleURIResolver;
import net.sf.saxon.trans.XPathException;

public class SystemIdCollectingModuleURIResolver extends StandardModuleURIResolver {

  private List<String> systemIds;
  
  public SystemIdCollectingModuleURIResolver(List<String> systemIds) {
    this.systemIds = systemIds;
  }

  @Override
  public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations) throws XPathException {
    StreamSource[] sources = super.resolve(moduleURI, baseURI, locations);
    if (systemIds != null) 
      for (StreamSource source : sources)
        systemIds.add(source.getSystemId());
    return sources;
  }
  
  public List<String> getSystemIds() {
    return systemIds;
  }
  
}