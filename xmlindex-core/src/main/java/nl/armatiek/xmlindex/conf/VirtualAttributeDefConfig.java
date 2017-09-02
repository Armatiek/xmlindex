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

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.w3c.dom.Element;

import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.util.XMLUtils;

public class VirtualAttributeDefConfig {
  
  private final HashMap<QName, Map<String, VirtualAttributeDef>> nameToVirtualAttrDefMap = new HashMap<QName, Map<String, VirtualAttributeDef>>();
  private final HashMap<String, VirtualAttributeDef> virtualAttributeDefs = new HashMap<String, VirtualAttributeDef>();
  private final Map<String, Analyzer> analyzerPerField;
  private final XMLIndex index;
  private XQueryFunctionLibrary functionLibrary;
  private XQueryEvaluator virtualAttrsEvaluator;
  
  public VirtualAttributeDefConfig(XMLIndex index, Element configElem, Map<String, Analyzer> analyzerPerField, Path analyzerConfigPath) {
    this.index = index;
    this.analyzerPerField = analyzerPerField;
    reloadXQueryModule();
    Element virtualAttributeConfigElem = XMLUtils.getFirstChildElementByLocalName(configElem, "virtual-attribute-config");
    if (virtualAttributeConfigElem == null)
      return;
    Element virtualAttributeDefElem = XMLUtils.getFirstChildElement(virtualAttributeConfigElem);
    while (virtualAttributeDefElem != null) {
      VirtualAttributeDef virtualAttributeDef = new VirtualAttributeDef(virtualAttributeDefElem, analyzerConfigPath);
      if (!functionExists(virtualAttributeDef.getFunctionName(), 1) && !functionExists(virtualAttributeDef.getFunctionName(), 2))
        throw new XMLIndexException("Error loading virtual attribute defintions. The virtual attribute function \"" + virtualAttributeDef.getFunctionName().getEQName() + "\""
            + " that was configured in \"" + Definitions.FILENAME_INDEXCONFIG + "\" was not defined in \"" + Definitions.FILENAME_VIRTATTRMODULE + "\".");
      cacheVirtualAttributeDef(virtualAttributeDef);
      virtualAttributeDefElem = XMLUtils.getNextSiblingElement(virtualAttributeDefElem);
    }  
  }
  
  public XQueryEvaluator getVirtualAttrsEvaluator() {
    return virtualAttrsEvaluator;
  }
  
  public Map<String, VirtualAttributeDef> getForElement(QName elemName) {
    return nameToVirtualAttrDefMap.get(elemName);
  }
  
  public VirtualAttributeDef get(String virtualAttributeName) {
    return virtualAttributeDefs.get(virtualAttributeName);
  }
  
  public Collection<VirtualAttributeDef> get() {
    return virtualAttributeDefs.values();
  }
  
  public boolean attributeExists(String virtualAttributeName) {
    return virtualAttributeDefs.containsKey(virtualAttributeName);
  }
  
  public boolean functionExists(QName functionName, int argCount) {
    return functionLibrary.getDeclaration(functionName.getStructuredQName(), argCount) != null;
  }
  
  public void reloadXQueryModule() {
    try {
      File xqueryFile = index.getConfigPath().resolve(Definitions.FILENAME_VIRTATTRMODULE).toFile();
      Processor proc = new Processor(false);
      XQueryCompiler compiler = proc.newXQueryCompiler();
      XQueryExecutable virtualAttrsExecutable = compiler.compile(xqueryFile);
      this.functionLibrary = virtualAttrsExecutable.getUnderlyingCompiledQuery().getMainModule().getGlobalFunctionLibrary();
      this.virtualAttrsEvaluator = virtualAttrsExecutable.load();
    } catch (Exception e) {
      throw new XMLIndexException("Error compiling virtual attribute definitions XQuery file for index \"" + index.getIndexName() + "\"", e);
    } 
  }
  
  private void cacheVirtualAttributeDef(VirtualAttributeDef attrDef) {
    for (QName elemName : attrDef.getElemNames()) {
      Map<String, VirtualAttributeDef> defs = nameToVirtualAttrDefMap.get(elemName);
      if (defs == null) {
        defs = new HashMap<String, VirtualAttributeDef>(1);
        nameToVirtualAttrDefMap.put(elemName, defs);
      }
      defs.put(attrDef.getVirtualAttributeName(), attrDef);
    }
    virtualAttributeDefs.put(attrDef.getVirtualAttributeName(), attrDef);
    if (attrDef.getIndexAnalyzer() != null)
      analyzerPerField.put(attrDef.getVirtualAttributeName(), attrDef.getIndexAnalyzer());
  }
  
}