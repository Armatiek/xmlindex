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

package nl.armatiek.xmlindex.extensions;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.w3c.dom.Element;

import net.sf.saxon.om.Sequence;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.util.XMLUtils;

public abstract class PluggableIndex {
  
  protected final XMLIndex index;
  protected Map<String, String> params;
  
  public PluggableIndex(XMLIndex index) {
    this.index = index;
  }
  
  public void init(Map<String, String> params) {
    this.params = params;
  }
  
  public Map<String, String> getParams() {
    return params;
  }
  
  public void close() { }
  
  public abstract PluggableIndexExtensionFunctionCall getFunctionCall();
  
  public abstract void indexNode(Document doc, Element node);
  
  public abstract Query getQuery(Sequence[] functionArguments);
  
  public static PluggableIndex fromClassName(XMLIndex index, String className, Map<String, String> params) throws Exception {
    Class<?> clazz = Class.forName(className);
    Constructor<?> ctor = clazz.getConstructor(XMLIndex.class);
    PluggableIndex pluggableIndex = (PluggableIndex) ctor.newInstance(new Object[] {index});
    pluggableIndex.init(params);
    return pluggableIndex;
  }
  
  public static PluggableIndex fromConfigElem(XMLIndex index, Element pluggableIndexDefElem) throws Exception {
    String className = XMLUtils.getValueOfChildElementByLocalName(pluggableIndexDefElem, "class-name");
    HashMap<String, String> params = new HashMap<String, String>();
    Element paramElem = XMLUtils.getChildElementByLocalName(pluggableIndexDefElem, "param");
    while (paramElem != null) {
      params.put(paramElem.getAttribute("name"), paramElem.getAttribute("value"));
      paramElem = XMLUtils.getNextSiblingElement(paramElem);
    }
    return fromClassName(index, className, params);
  }
  
  public void reindex() throws IOException {
    Session session = index.aquireSession();
    try {  
      session.reindexPluggableIndex(this);
    } finally {
      index.returnSession(session);
    }
  }

}