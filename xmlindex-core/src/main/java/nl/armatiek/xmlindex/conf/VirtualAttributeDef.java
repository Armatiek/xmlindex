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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XdmNode;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.utils.XMLUtils;

public class VirtualAttributeDef {
  
  protected final String name;
  protected final XMLIndex index;
  protected final String patternStr;
  protected XPathSelector selector;
  protected final List<VirtualAttribute> virtualAttributes = new ArrayList<VirtualAttribute>();
  protected final List<NamespaceBinding> namespaceBindings = new ArrayList<NamespaceBinding>();
  
  public VirtualAttributeDef(XMLIndex index, Processor processor, Element virtualAttributeDefElem, 
      Path analyzerConfigPath, XQueryEvaluator eval) throws SaxonApiException {
    this.index = index;
    this.name = XMLUtils.getValueOfChildElementByLocalName(virtualAttributeDefElem, "name");
    Element patternElem = XMLUtils.getChildElementByLocalName(virtualAttributeDefElem, "pattern");  
    XPathCompiler compiler = processor.newXPathCompiler();
    HashMap<String, String> declarations = new HashMap<String, String>();
    XMLUtils.getNamespaceDeclarationsInScope(patternElem, declarations);
    Iterator<Entry<String, String>> iter = declarations.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, String> pair = (Entry<String, String>) iter.next();
      namespaceBindings.add(new NamespaceBinding(pair.getValue(), pair.getKey()));
      compiler.declareNamespace(pair.getValue(), pair.getKey());
    }
    patternStr = patternElem.getTextContent();
    XPathExecutable exec = compiler.compilePattern(patternStr);
    selector = exec.load();
    NodeList vaList = virtualAttributeDefElem.getElementsByTagName("virtual-attribute");
    for (int i=0; i<vaList.getLength(); i++)
      virtualAttributes.add(new VirtualAttribute((Element) vaList.item(i), analyzerConfigPath, eval)); 
  }
  
  public VirtualAttributeDef(XMLIndex index) {
    this.name = "";
    this.index = index;
    this.selector = null;
    this.patternStr = null;
  }
  
  public String getName() {
    return name;
  }
  
  public boolean matches(XdmNode node) throws SaxonApiException {
    selector.setContextItem(node);
    return selector.effectiveBooleanValue();
  }
  
  public List<VirtualAttribute> getVirtualAttributes() {
    return virtualAttributes;
  }
  
  public List<NamespaceBinding> getNamespaceBindings() {
    return namespaceBindings;
  }
  
  public String getPattern() {
    return patternStr;
  }
  
  public void reindex() throws IOException {
    Session session = index.aquireSession();
    try {  
      session.reindexVirtualAttributeDef(this);
    } finally {
      index.returnSession(session);
    }
  }
  
}