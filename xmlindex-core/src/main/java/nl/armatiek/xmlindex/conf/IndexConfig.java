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
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class IndexConfig {
  
  private final TypedValueDefConfig typedValueConfig;
  private final VirtualAttributeDefConfig virtualAttributeConfig;
  private final PluggableIndexConfig pluggableIndexConfig;
  private final Path analyzerConfigPath;
  
  public IndexConfig(XMLIndex index, Map<String, Analyzer> analyzerPerField) {
    try {
      this.analyzerConfigPath = index.getIndexPath().resolve(Definitions.FOLDERNAME_ANALYZER);
      this.typedValueConfig = new TypedValueDefConfig(index);
      this.virtualAttributeConfig = new VirtualAttributeDefConfig(index, analyzerPerField, analyzerConfigPath);    
      this.pluggableIndexConfig = new PluggableIndexConfig(index);
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
  
}
