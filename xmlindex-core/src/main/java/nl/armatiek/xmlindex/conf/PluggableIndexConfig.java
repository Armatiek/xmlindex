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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.StructuredQName;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.extensions.PluggableIndex;
import nl.armatiek.xmlindex.extensions.PluggableIndexExtensionFunctionCall;
import nl.armatiek.xmlindex.util.XMLUtils;

public class PluggableIndexConfig {
  
  private final List<PluggableIndex> pluggableIndexes = new ArrayList<PluggableIndex>(); 
  private final HashMap<StructuredQName, PluggableIndex> pluggableIndexesCallMap = new HashMap<StructuredQName, PluggableIndex>();
  
  public PluggableIndexConfig(XMLIndex index, Element configElem) {
    try {
      Element pluggableIndexConfigElem = XMLUtils.getFirstChildElementByLocalName(configElem, "pluggable-index-config");
      if (pluggableIndexConfigElem == null)
        return;
      Element pluggableIndexDefElem = XMLUtils.getFirstChildElement(pluggableIndexConfigElem);
      while (pluggableIndexDefElem != null) {
        PluggableIndex pluggableIndex = PluggableIndex.fromConfigElem(index, pluggableIndexDefElem);
        pluggableIndexes.add(pluggableIndex);
        pluggableIndexesCallMap.put(pluggableIndex.getFunctionCall().getDefinition().getFunctionQName(), pluggableIndex);
        ExtensionFunctionDefinition extensionFunction = pluggableIndex.getFunctionCall().getDefinition();
        index.getSaxonConfiguration().registerExtensionFunction(extensionFunction);
        pluggableIndexDefElem = XMLUtils.getNextSiblingElement(pluggableIndexDefElem);
      }
    } catch (Exception e) {
      throw new XMLIndexException("Error initializing pluggable indexes for index \"" + index.getIndexName() + "\"", e);
    } 
  }
   
  public Collection<PluggableIndex> get() {
    return pluggableIndexes;
  }
  
  public PluggableIndex get(PluggableIndexExtensionFunctionCall call) {
    return pluggableIndexesCallMap.get(call.getDefinition().getFunctionQName());
  }
 
}