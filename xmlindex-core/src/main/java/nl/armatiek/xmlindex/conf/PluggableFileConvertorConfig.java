/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.armatiek.xmlindex.conf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.plugins.convertor.PluggableFileConvertor;
import nl.armatiek.xmlindex.utils.XMLUtils;

public class PluggableFileConvertorConfig {
  
  private final Map<String, PluggableFileConvertor> pluggableFileConvertors = new HashMap<String, PluggableFileConvertor>(); 
  
  public PluggableFileConvertorConfig(XMLIndex index, Element configElem) {
    try {
      Element pluggableFileConvertorConfigElem = XMLUtils.getFirstChildElementByLocalName(configElem, "pluggable-file-convertor-config");
      if (pluggableFileConvertorConfigElem == null)
        return;
      Element pluggableFileConvertorDefElem = XMLUtils.getFirstChildElement(pluggableFileConvertorConfigElem);
      while (pluggableFileConvertorDefElem != null) {
        PluggableFileConvertor convertor = (PluggableFileConvertor) PluggableFileConvertor.fromConfigElem(pluggableFileConvertorDefElem);
        pluggableFileConvertors.put(convertor.getName(), convertor);
        pluggableFileConvertorDefElem = XMLUtils.getNextSiblingElement(pluggableFileConvertorDefElem);
      }
    } catch (Exception e) {
      throw new XMLIndexException("Error initializing pluggable file convertor for index \"" + index.getIndexName() + "\"", e);
    } 
  }
   
  public Collection<PluggableFileConvertor> getConvertors() {
    return pluggableFileConvertors.values();
  }
  
  public PluggableFileConvertor getByName(String name) {
    return pluggableFileConvertors.get(name);
  }
  
}