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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XdmNode;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.utils.XMLUtils;

public class VirtualAttributeDef2 {
  
  protected final XMLIndex index;
  protected XPathSelector selector;
  protected final List<VirtualAttributeDef> virtualAttributes = new ArrayList<VirtualAttributeDef>();
  
  public VirtualAttributeDef2(XMLIndex index, Processor processor, Element virtualAttributeDefElem, 
      Path analyzerConfigPath, XQueryEvaluator eval) throws SaxonApiException {
    this.index = index;
    Element patternElem = XMLUtils.getChildElementByLocalName(virtualAttributeDefElem, "pattern");  
    XPathCompiler compiler = processor.newXPathCompiler();
    HashMap<String, String> declarations = new HashMap<String, String>();
    XMLUtils.getNamespaceDeclarationsInScope(patternElem, declarations);
    Iterator<Entry<String, String>> iter = declarations.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, String> pair = (Entry<String, String>) iter.next();
      compiler.declareNamespace(pair.getValue(), pair.getKey());
    }
    XPathExecutable exec = compiler.compilePattern(patternElem.getTextContent());
    selector = exec.load();
    NodeList vaList = virtualAttributeDefElem.getElementsByTagName("virtual-attribute");
    for (int i=0; i<vaList.getLength(); i++)
      virtualAttributes.add(new VirtualAttributeDef(index, (Element) vaList.item(i), analyzerConfigPath, eval)); 
  }
  
  public VirtualAttributeDef2(XMLIndex index) {
    this.index = index;
    this.selector = null;
  }
  
  public boolean matches(XdmNode node) throws SaxonApiException {
    selector.setContextItem(node);
    return selector.effectiveBooleanValue();
  }
  
  public List<VirtualAttributeDef> getVirtualAttributes() {
    return virtualAttributes;
  }
  
}