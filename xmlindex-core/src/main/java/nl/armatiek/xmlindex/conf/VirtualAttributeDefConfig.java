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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class VirtualAttributeDefConfig {
  
  private static final Logger logger = LoggerFactory.getLogger(VirtualAttributeDefConfig.class);
  
  private final HashMap<QName, Map<String, VirtualAttributeDef>> nameToVirtualAttrDefMap = new HashMap<QName, Map<String, VirtualAttributeDef>>();
  private final HashMap<String, VirtualAttributeDef> virtualAttributeDefs = new HashMap<String, VirtualAttributeDef>();
  private final Map<String, Analyzer> analyzerPerField;
  private final XMLIndex index;
  private XQueryEvaluator virtualAttrsEvaluator;
  
  public VirtualAttributeDefConfig(XMLIndex index, Map<String, Analyzer> analyzerPerField, Path analyzerConfigPath) {
    try {
      this.index = index;
      this.analyzerPerField = analyzerPerField;
      File analyzerDir;
      if (!(analyzerDir = new File(index.getIndexPath().toFile(), Definitions.FOLDERNAME_ANALYZER)).exists()) {
        if (!analyzerDir.mkdir())
          throw new XMLIndexException("Unable to create directory \"" + analyzerDir.getAbsolutePath() + "\"");
      }
      reloadXQueryModule();
      List<VirtualAttributeDef> attrDefs = index.getNodeStore().getVirtualAttributeDefs(analyzerConfigPath);
      for (VirtualAttributeDef attrDef : attrDefs)
        cacheVirtualAttributeDef(attrDef);
    } catch (XMLIndexException xie) {
      throw xie;
    } catch (Exception e) {
      throw new XMLIndexException("Error initializing virtual attribute definitions for index \"" + index.getIndexName() + "\"", e);
    }  
  }
  
  public XQueryEvaluator getVirtualAttrsEvaluator() {
    return virtualAttrsEvaluator;
  }
  
  public void add(VirtualAttributeDef attrDef) throws IOException {
    if (virtualAttributeDefs.containsKey(attrDef.getVirtualAttributeName())) 
      throw new IllegalArgumentException("A virtual attribute definition for the attribute name \"" + attrDef.getVirtualAttributeName() + "\" already exists");
    logger.info("Adding virtual attribute definition \"" + attrDef.getVirtualAttributeName() + "\" ...");
    cacheVirtualAttributeDef(attrDef);  
    attrDef.store();
    logger.info("Added virtual attribute definition \"" + attrDef.getVirtualAttributeName() + "\"");
  }
  
  public void remove(VirtualAttributeDef attrDef) throws IOException {
    if (!exists(attrDef.getVirtualAttributeName())) 
      throw new IllegalArgumentException("A virtual attribute definition for the attribute name \"" + attrDef.getVirtualAttributeName() + "\" does not exists");
    index.getNodeStore().deleteLuceneDocument(new Term(Definitions.FIELDNAME_DEFNAME, attrDef.getVirtualAttributeName()));
    index.getNodeStore().commit(true);
    virtualAttributeDefs.remove(attrDef.getVirtualAttributeName());
    nameToVirtualAttrDefMap.remove(attrDef.getElemName());
    analyzerPerField.remove(attrDef.getVirtualAttributeName());
    logger.info("Removed virtual attribute definition \"" + attrDef.getVirtualAttributeName() + "\"");
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
  
  public boolean exists(String virtualAttributeName) {
    return virtualAttributeDefs.containsKey(virtualAttributeName);
  }
  
  public void reloadXQueryModule() {
    try {
      File xqueryFile = index.getIndexPath().resolve(Definitions.FILENAME_VIRTATTRMODULE).toFile();
      if (!xqueryFile.isFile())
        FileUtils.writeStringToFile(xqueryFile, "xquery version \"3.1\";\n\n()", StandardCharsets.UTF_8);
      Processor proc = new Processor(false);
      XQueryCompiler compiler = proc.newXQueryCompiler();
      XQueryExecutable xqueryExec = compiler.compile(xqueryFile); 
      virtualAttrsEvaluator = xqueryExec.load();
    } catch (Exception e) {
      throw new XMLIndexException("Error compiling virtual attribute definitions XQuery file for index \"" + index.getIndexName() + "\"", e);
    } 
  }
  
  private void cacheVirtualAttributeDef(VirtualAttributeDef attrDef) {
    Map<String, VirtualAttributeDef> defs = nameToVirtualAttrDefMap.get(attrDef.getElemName());
    if (defs == null) {
      defs = new HashMap<String, VirtualAttributeDef>(1);
      nameToVirtualAttrDefMap.put(attrDef.getElemName(), defs);
    }
    defs.put(attrDef.getVirtualAttributeName(), attrDef);
    virtualAttributeDefs.put(attrDef.getVirtualAttributeName(), attrDef);
    if (attrDef.getIndexAnalyzer() != null)
      analyzerPerField.put(attrDef.getVirtualAttributeName(), attrDef.getIndexAnalyzer());
  }
  
}