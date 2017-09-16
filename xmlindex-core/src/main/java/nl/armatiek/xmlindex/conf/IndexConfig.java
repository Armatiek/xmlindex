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
import java.nio.file.Path;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;

import org.apache.lucene.analysis.Analyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.utils.XMLUtils;

public class IndexConfig extends ConfigBase implements ErrorHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(IndexConfig.class);
  
  private final Configuration configuration;
  private final Processor processor;
  private final boolean developmentMode;
  private final TypedValueDefConfig typedValueConfig;
  private final VirtualAttributeDefConfig virtualAttributeConfig;
  private final PluggableIndexConfig pluggableIndexConfig;
  private final PluggableFileConvertorConfig pluggableFileConvertorConfig;
  private final Path analyzerConfigPath;
  private final XMLIndex index;
  
  public IndexConfig(XMLIndex index, Map<String, Analyzer> analyzerPerField, Schema configSchema) {
    try {
      this.index = index;
      this.configuration = new Configuration();
      this.processor = new Processor(configuration);
      File configFile = index.getConfigPath().resolve(Definitions.FILENAME_INDEXCONFIG).toFile();
      if (!configFile.isFile())
        copyFromClassPath("template-index-configuration.xml", configFile, null);
      File virtAttrsFile = index.getConfigPath().resolve(Definitions.FILENAME_VIRTATTRMODULE).toFile();
      if (!virtAttrsFile.isFile())
        copyFromClassPath("template-virtual-attributes.xqy", virtAttrsFile, null);
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
      this.developmentMode = XMLUtils.getBooleanValue(XMLUtils.getValueOfChildElementByLocalName(configElem, "development-mode"), false);
      this.typedValueConfig = new TypedValueDefConfig(configElem);
      this.virtualAttributeConfig = new VirtualAttributeDefConfig(index, processor, configElem, analyzerPerField, analyzerConfigPath);    
      this.pluggableIndexConfig = new PluggableIndexConfig(index, configElem);
      this.pluggableFileConvertorConfig = new PluggableFileConvertorConfig(index, configElem);
    } catch (XMLIndexException e) {
      throw e;
    } catch (Exception e) {
      throw new XMLIndexException("Error initializing index \"" + index.getIndexName() + "\"", e);
    }
  }
  
  public Configuration getConfiguration() {
    return configuration;
  }
  
  public Processor getProcessor() {
    return processor;
  }
  
  public boolean getDevelopmentMode() {
    return developmentMode;
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
  
  public PluggableFileConvertorConfig getPluggableFileConvertorConfig() {
    return pluggableFileConvertorConfig;
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
