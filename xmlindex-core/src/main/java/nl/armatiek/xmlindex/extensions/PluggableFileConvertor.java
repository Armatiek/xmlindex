package nl.armatiek.xmlindex.extensions;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.util.XMLUtils;

public abstract class PluggableFileConvertor {

  protected final XMLIndex index;
  protected Map<String, String> params;
  
  public PluggableFileConvertor(XMLIndex index) {
    this.index = index;
  }
  
  public void init(Map<String, String> params) {
    this.params = params;
  }
  
  public Map<String, String> getParams() {
    return params;
  }
  
  public void close() { }
  
  public abstract Set<String> getSupportedExtensions();
  
  public abstract void convertFile(InputStream is, OutputStream os, String encoding) throws Exception;
  
  public abstract void convertFolder(Path path, OutputStream os, String encoding) throws Exception;
  
  public static PluggableFileConvertor fromClassName(XMLIndex index, String className, Map<String, String> params) throws Exception {
    Class<?> clazz = Class.forName(className);
    Constructor<?> ctor = clazz.getConstructor(XMLIndex.class);
    PluggableFileConvertor pluggableFileConvertor = (PluggableFileConvertor) ctor.newInstance(new Object[] {index});
    pluggableFileConvertor.init(params);
    return pluggableFileConvertor;
  }
  
  public static PluggableFileConvertor fromConfigElem(XMLIndex index, Element pluggableIndexDefElem) throws Exception {
    String className = XMLUtils.getValueOfChildElementByLocalName(pluggableIndexDefElem, "class-name");
    HashMap<String, String> params = new HashMap<String, String>();
    Element paramElem = XMLUtils.getChildElementByLocalName(pluggableIndexDefElem, "param");
    while (paramElem != null) {
      params.put(paramElem.getAttribute("name"), paramElem.getAttribute("value"));
      paramElem = XMLUtils.getNextSiblingElement(paramElem);
    }
    return fromClassName(index, className, params);
  }
  
}
