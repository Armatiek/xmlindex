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
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.w3c.dom.Element;

import net.sf.saxon.om.Sequence;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.extensions.CustomIndexExtensionFunctionCall;

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
  
  public abstract CustomIndexExtensionFunctionCall getFunctionCall();
  
  public abstract void indexNode(Document doc, Element node);
  
  public abstract Query getQuery(Sequence[] functionArguments);
  
  public static PluggableIndex fromClassName(XMLIndex index, String className, Map<String, String> params) throws Exception {
    Class<?> clazz = Class.forName(className);
    Constructor<?> ctor = clazz.getConstructor(XMLIndex.class);
    PluggableIndex pluggableIndex = (PluggableIndex) ctor.newInstance(new Object[] {index});
    pluggableIndex.init(params);
    return pluggableIndex;
  }
  
  public static PluggableIndex fromDocument(XMLIndex index, Document doc) throws Exception {
    String className = doc.get(Definitions.FIELDNAME_CLASSNAME);
    HashMap<String, String> params = new HashMap<String, String>();
    for (IndexableField field : doc.getFields(Definitions.FIELDNAME_PARAMS)) {
      String value = field.stringValue();
      params.put(StringUtils.substringBefore(value, "||"), StringUtils.substringAfter(value, "||"));
    }
    return fromClassName(index, className, params);
  }
  
  public void store() throws IOException {
    Document doc = new Document();
    doc.add(new NumericDocValuesField(Definitions.FIELDNAME_LEFT, Definitions.FIELVALUE_CONFIG_LEFT));
    doc.add(new StringField(Definitions.FIELDNAME_INDEXINFO, Definitions.FIELDVALUE_PLUGGABLEINDEX, Store.NO));
    doc.add(new StoredField(Definitions.FIELDNAME_CLASSNAME, getClass().getName())); 
    if (params != null)
      for (Map.Entry<String, String> entry : params.entrySet())
        doc.add(new StoredField(Definitions.FIELDNAME_PARAMS, entry.getKey() + "||" + entry.getValue()));
    index.getNodeStore().updateLuceneDocument(new Term(Definitions.FIELDNAME_DEFNAME, getClass().getName()), doc);
    index.getNodeStore().commit(true);
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