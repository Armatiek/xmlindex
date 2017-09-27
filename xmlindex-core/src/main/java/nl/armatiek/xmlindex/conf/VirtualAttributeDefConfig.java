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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;

import org.apache.lucene.analysis.Analyzer;
import org.w3c.dom.Element;

import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.type.Type;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.utils.XMLUtils;

public class VirtualAttributeDefConfig extends ConfigBase {
  
  private final Map<String, VirtualAttributeDef> virtualAttributeDefs = new HashMap<String, VirtualAttributeDef>();
  private BuiltInVirtualAttributeDef builtInVirtualAttributeDef;
  private final HashMap<String, VirtualAttribute> virtualAttributes = new HashMap<String, VirtualAttribute>();
  
  private final Map<String, Analyzer> analyzerPerField;
  private final XMLIndex index;
  private final Processor processor;
  private XQueryFunctionLibrary functionLibrary;
  private XQueryEvaluator virtualAttrsEvaluator;
  private XQueryEvaluator buildInVirtualAttrsEvaluator;
  
  public VirtualAttributeDefConfig(XMLIndex index, Processor processor, Element configElem, 
      Map<String, Analyzer> analyzerPerField, Path analyzerConfigPath) throws SaxonApiException {
    this.index = index;
    this.processor = processor;
    this.analyzerPerField = analyzerPerField;
    Element virtualAttributeConfigElem = XMLUtils.getFirstChildElementByLocalName(configElem, "virtual-attribute-config");
    Element virtualAttributeDefElem = XMLUtils.getFirstChildElement(virtualAttributeConfigElem);
    if (virtualAttributeDefElem == null)
      return;
    
    reloadXQueryModule(processor);
    reloadBuiltInXQueryModule(processor);
    
    while (virtualAttributeDefElem != null) {
      VirtualAttributeDef virtualAttributeDef = new VirtualAttributeDef(index, processor, virtualAttributeDefElem, 
          analyzerConfigPath, virtualAttrsEvaluator);
      Collection<VirtualAttribute> vads = virtualAttributeDef.getVirtualAttributes();
      for (VirtualAttribute vad : vads) {
        if (!functionExists(vad.getFunctionName(), 1) && !functionExists(vad.getFunctionName(), 2))
          throw new XMLIndexException("Error loading virtual attribute defintions. The virtual attribute function \"" + vad.getFunctionName().getEQName() + "\""
              + " that was configured in \"" + Definitions.FILENAME_INDEXCONFIG + "\" was not defined in \"" + Definitions.FILENAME_VIRTATTRMODULE + "\".");
        if (vad.getIndexAnalyzer() != null)
          analyzerPerField.put(vad.getVirtualAttributeName(), vad.getIndexAnalyzer());
        virtualAttributes.put(vad.getVirtualAttributeName(), vad);
      }
      virtualAttributeDefs.put(virtualAttributeDef.getName(), virtualAttributeDef);
      virtualAttributeDefElem = XMLUtils.getNextSiblingElement(virtualAttributeDefElem);
    }
    
    loadBuiltInVirtualAttributeDef();
  }
  
  public Collection<VirtualAttribute> getForElement(XdmNode node) throws SaxonApiException {
    List<VirtualAttribute> vads = null;
    for (VirtualAttributeDef vad : virtualAttributeDefs.values()) {
      if (vad.matches(node)) {
        if (vads == null)
          vads = new ArrayList<VirtualAttribute>();
        vads.addAll(vad.getVirtualAttributes());
      }
    }
    return vads;
  }
  
  public BuiltInVirtualAttributeDef getBuiltInVirtualAttributeDef() {
    return builtInVirtualAttributeDef;
  }
  
  public VirtualAttributeDef getVirtualAttributeDef(String name) {
    return virtualAttributeDefs.get(name);
  }
  
  public VirtualAttribute getVirtualAttribute(String virtualAttributeName) {
    return virtualAttributes.get(virtualAttributeName);
  }
  
  public boolean attributeExists(String virtualAttributeName) {
    return virtualAttributes.containsKey(virtualAttributeName);
  }
  
  public boolean functionExists(QName functionName, int argCount) {
    return functionLibrary.getDeclaration(functionName.getStructuredQName(), argCount) != null;
  }
  
  public void reloadXQueryModule(Processor proc) {
    try {
      File xqueryFile = index.getConfigPath().resolve(Definitions.FILENAME_VIRTATTRMODULE).toFile();
      XQueryCompiler compiler = proc.newXQueryCompiler();
      XQueryExecutable virtualAttrsExecutable = compiler.compile(xqueryFile);
      this.functionLibrary = virtualAttrsExecutable.getUnderlyingCompiledQuery().getMainModule().getGlobalFunctionLibrary();
      this.virtualAttrsEvaluator = virtualAttrsExecutable.load();
    } catch (Exception e) {
      throw new XMLIndexException("Error compiling virtual attribute definitions XQuery file for index \"" + index.getIndexName() + "\"", e);
    } 
  }
  
  public void reloadBuiltInXQueryModule(Processor proc) {
    try {
      String xquery = this.readFromClassPath("document-element-virtual-attributes.xqy", null);
      XQueryCompiler compiler = proc.newXQueryCompiler();
      XQueryExecutable virtualAttrsExecutable = compiler.compile(xquery);
      this.buildInVirtualAttrsEvaluator = virtualAttrsExecutable.load();
    } catch (Exception e) {
      throw new XMLIndexException("Error compiling standard virtual attribute definitions XQuery file for index \"" + index.getIndexName() + "\"", e);
    } 
  }
  
  private void loadBuiltInVirtualAttributeDef() throws SaxonApiException {
    builtInVirtualAttributeDef = new BuiltInVirtualAttributeDef(index, processor, buildInVirtualAttrsEvaluator);
    for (String[] attr : Definitions.STANDARD_ATTRS) {
      VirtualAttribute vad =  
          new VirtualAttribute(
            attr[0], 
            new QName(Definitions.NAMESPACE_VIRTUALATTR, "_" + attr[0]), 
            Type.getBuiltInItemType(XMLConstants.W3C_XML_SCHEMA_NS_URI, attr[1]), 
            true,
            buildInVirtualAttrsEvaluator);
      virtualAttributes.put(vad.getVirtualAttributeName(), vad);
      if (vad.getIndexAnalyzer() != null)
        analyzerPerField.put(vad.getVirtualAttributeName(), vad.getIndexAnalyzer());
      builtInVirtualAttributeDef.addVirtualAttribute(vad);
    }
  }
  
}