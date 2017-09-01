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
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class IndexConfig implements ErrorHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(IndexConfig.class);
  
  private final TypedValueDefConfig typedValueConfig;
  private final VirtualAttributeDefConfig virtualAttributeConfig;
  private final PluggableIndexConfig pluggableIndexConfig;
  private final Path analyzerConfigPath;
  private final XMLIndex index;
  
  public IndexConfig(XMLIndex index, Map<String, Analyzer> analyzerPerField, Schema configSchema) {
    try {
      this.index = index;
      File configFile = index.getConfigPath().resolve(Definitions.FILENAME_INDEXCONFIG).toFile();
      if (!configFile.isFile())
        FileUtils.writeStringToFile(configFile, 
            "<?xml version=\"1.0\"?>" + Definitions.EOL + "<index-configuration/>", StandardCharsets.UTF_8);
      
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      if (configSchema != null)
        dbf.setSchema(configSchema);    
      dbf.setXIncludeAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      db.setErrorHandler(this);
      Element configElem = db.parse(configFile).getDocumentElement();
      
      File analyzerConfigDir = new File(index.getConfigPath().toFile(), Definitions.FOLDERNAME_ANALYZER);
      if (!analyzerConfigDir.isDirectory() && !analyzerConfigDir.mkdirs())
        throw new FileNotFoundException("Error creating directory \"" + analyzerConfigDir.getAbsolutePath() + "\"");
      this.analyzerConfigPath = analyzerConfigDir.toPath();
      this.typedValueConfig = new TypedValueDefConfig(configElem);
      this.virtualAttributeConfig = new VirtualAttributeDefConfig(index, configElem, analyzerPerField, analyzerConfigPath);    
      this.pluggableIndexConfig = new PluggableIndexConfig(index, configElem);
    } catch (XMLIndexException e) {
      throw e;
    } catch (Exception e) {
      throw new XMLIndexException("Error initializing index \"" + index.getIndexName() + "\"", e);
    }
  }
  
  public TypedValueDefConfig getTypedValueConfig() {
    return typedValueConfig;
  }
  
  public VirtualAttributeDefConfig getVirtualAttributeConfig() {
    return virtualAttributeConfig;
  }
  
  public PluggableIndexConfig getPluggableIndexConfig() {
    return pluggableIndexConfig;
  }
  
  public Path getAnalyzerConfigPath() {
    return analyzerConfigPath;
  }

  @Override
  public void warning(SAXParseException exception) throws SAXException {
    logger.warn("Warning parsing configuration file of index \"" + index.getIndexName() + "\"", exception);
  }

  @Override
  public void error(SAXParseException exception) throws SAXException {
    logger.warn("Error parsing configuration file of index \"" + index.getIndexName() + "\"", exception);
    throw exception;
  }

  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    logger.warn("Error parsing configuration file of index \"" + index.getIndexName() + "\"", exception);
    throw exception;
  }
  
}
