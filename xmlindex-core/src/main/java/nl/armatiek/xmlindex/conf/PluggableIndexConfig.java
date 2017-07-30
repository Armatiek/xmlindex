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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.StructuredQName;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.extensions.CustomIndexExtensionFunctionCall;

public class PluggableIndexConfig {
  
  private static final Logger logger = LoggerFactory.getLogger(PluggableIndexConfig.class);
  
  private final Map<String, PluggableIndex> pluggableIndexes = new HashMap<String, PluggableIndex>(); 
  private final HashMap<StructuredQName, PluggableIndex> pluggableIndexesCallMap = new HashMap<StructuredQName, PluggableIndex>();
  private final XMLIndex index;
  
  public PluggableIndexConfig(XMLIndex index) {
    try {
      this.index = index;
      List<PluggableIndex> pluggableIndexes = index.getNodeStore().getPluggableIndexes();
      for (PluggableIndex pluggableIndex : pluggableIndexes) {
        this.pluggableIndexes.put(pluggableIndex.getClass().getName(), pluggableIndex);
        pluggableIndexesCallMap.put(pluggableIndex.getFunctionCall().getDefinition().getFunctionQName(), pluggableIndex);
        ExtensionFunctionDefinition extensionFunction = pluggableIndex.getFunctionCall().getDefinition();
        index.getSaxonConfiguration().registerExtensionFunction(extensionFunction);
      }
    } catch (Exception e) {
      throw new XMLIndexException("Error initializing pluggable indexes for index \"" + index.getIndexName() + "\"", e);
    } 
  }
  
  public void add(PluggableIndex pluggableIndex) throws IOException {
    if (pluggableIndexes.containsKey(pluggableIndex.getClass().getName())) 
      throw new IllegalArgumentException("Pluggable index of class \"" + pluggableIndex.getClass().getName() + "\" already exists");
    logger.info("Adding pluggable index \"" + pluggableIndex.getClass().getName() + "\" ...");
    pluggableIndex.store();
    pluggableIndexes.put(pluggableIndex.getClass().getName(), pluggableIndex);
    pluggableIndexesCallMap.put(pluggableIndex.getFunctionCall().getDefinition().getFunctionQName(), pluggableIndex);
    ExtensionFunctionDefinition extensionFunction = pluggableIndex.getFunctionCall().getDefinition();
    index.getSaxonConfiguration().registerExtensionFunction(extensionFunction);
    logger.info("Added pluggable index \"" + pluggableIndex.getClass().getName() + "\"");
  }
  
  public void remove(PluggableIndex pluggableIndex) throws IOException {
    String className = pluggableIndex.getClass().getName();
    if (!pluggableIndexes.containsKey(className)) 
      throw new IllegalArgumentException("Pluggable index \"" + className + "\" does not exists");
    index.getNodeStore().deleteLuceneDocument(new Term(Definitions.FIELDNAME_DEFNAME, className));
    index.getNodeStore().commit(true);
    pluggableIndexes.remove(className);
    pluggableIndexesCallMap.remove(pluggableIndex.getFunctionCall().getDefinition().getFunctionQName());
    logger.info("Pluggable index \"" + className + "\" removed");
  }
   
  public PluggableIndex get(String className) {
    return pluggableIndexes.get(className);
  }
  
  public Collection<PluggableIndex> get() {
    return pluggableIndexes.values();
  }
  
  public PluggableIndex get(CustomIndexExtensionFunctionCall call) {
    return pluggableIndexesCallMap.get(call.getDefinition().getFunctionQName());
  }
  
  public boolean exists(String pluggableIndexClassName) {
    return pluggableIndexes.containsKey(pluggableIndexClassName);
  }

}