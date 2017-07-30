package nl.armatiek.xmlindex.test;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.IndexConfig;
import nl.armatiek.xmlindex.extensions.SpatialIndex;

public abstract class TestBase {
  
  protected void configure(XMLIndex index) throws Exception {
    IndexConfig config = index.getConfiguration();
    String className = "nl.armatiek.xmlindex.extensions.SpatialIndex";
    if (config.getPluggableIndexConfig().exists(className))
      return;
    Class<?> clazz = Class.forName(className);
    Constructor<?> ctor = clazz.getConstructor(XMLIndex.class);
    SpatialIndex spatialIndex = (SpatialIndex) ctor.newInstance(new Object[] {index});
    HashMap<String, String> params = new HashMap<String, String>();
    params.put(SpatialIndex.PROPERTYNAME_FIELDNAME, "shape");
    spatialIndex.init(params);
    config.getPluggableIndexConfig().add(spatialIndex);
  }

}