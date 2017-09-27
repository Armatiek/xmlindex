package nl.armatiek.xmlindex.plugins;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import nl.armatiek.xmlindex.utils.XMLUtils;

public abstract class XMLIndexPlugin {
  
  protected final String name;
  protected Map<String, String> params;
  
  public XMLIndexPlugin(String name) {
    this.name = name;
  }
  
  public void init(Map<String, String> params) throws Exception {
    this.params = params;
  }
  
  public String getName() {
    return name;
  }
  
  public Map<String, String> getParams() {
    return params;
  }
  
  public void close() { }
  
  public static XMLIndexPlugin fromClassName(String name, String className, Map<String, String> params) throws Exception {
    Class<?> clazz = Class.forName(className);
    Constructor<?> ctor = clazz.getConstructor(String.class);
    XMLIndexPlugin plugin = (XMLIndexPlugin) ctor.newInstance(new Object[] { name });
    plugin.init(params);
    return plugin;
  }
  
  public static XMLIndexPlugin fromConfigElem(Element pluginElem) throws Exception {
    String name = XMLUtils.getValueOfChildElementByLocalName(pluginElem, "name");
    String className = XMLUtils.getValueOfChildElementByLocalName(pluginElem, "class-name");
    HashMap<String, String> params = new HashMap<String, String>();
    Element paramElem = XMLUtils.getChildElementByLocalName(pluginElem, "param");
    while (paramElem != null) {
      params.put(paramElem.getAttribute("name"), paramElem.getAttribute("value"));
      paramElem = XMLUtils.getNextSiblingElement(paramElem);
    }
    return fromClassName(name, className, params);
  }
  
}