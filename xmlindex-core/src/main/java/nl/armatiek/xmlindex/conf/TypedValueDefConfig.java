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

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import net.sf.saxon.s9api.QName;
import nl.armatiek.xmlindex.util.XMLUtils;

public class TypedValueDefConfig {
  
  private final HashMap<String, TypedValueDef> typedValueDefs = new HashMap<String, TypedValueDef>(); 
  
  public TypedValueDefConfig(Element configElem) {
    Element typedValueConfigElem = XMLUtils.getFirstChildElementByLocalName(configElem, "typed-value-config");
    if (typedValueConfigElem == null)
      return;
    Element typedValueDefElem = XMLUtils.getFirstChildElement(typedValueConfigElem);
    while (typedValueDefElem != null) {
      TypedValueDef typedValueDef = new TypedValueDef(typedValueDefElem);
      typedValueDefs.put(typedValueDef.getKey(), typedValueDef);
      typedValueDefElem = XMLUtils.getNextSiblingElement(typedValueDefElem);
    } 
  }
  
  public Collection<TypedValueDef> get() {
    return typedValueDefs.values();
  }
  
  public TypedValueDef get(int nodeType, QName name) {
    String key = nodeType + name.getNamespaceURI() + name.getLocalName();
    return typedValueDefs.get(key);
  }
  
  public TypedValueDef get(int nodeType, String namespaceURI, String localName) {
    String key = nodeType + StringUtils.defaultString(namespaceURI) + localName;
    return typedValueDefs.get(key);
  }
  
  public boolean exists(int nodeType, QName name) {
    return get(nodeType, name) != null;
  }
  
}