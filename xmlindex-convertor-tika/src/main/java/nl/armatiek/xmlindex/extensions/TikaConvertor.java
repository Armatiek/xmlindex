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

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.xml.sax.ContentHandler;

import nl.armatiek.xmlindex.plugins.convertor.PluggableFileConvertor;

public class TikaConvertor extends PluggableFileConvertor {
  
  private final AutoDetectParser parser;

  public TikaConvertor(String name) {
    super(name);
    parser = new AutoDetectParser();
  }
  
  @Override
  public void convert(InputStream is, String systemId, OutputStream xmlStream) throws Exception {
    ContentHandler handler = new ToXMLContentHandler(xmlStream, "UTF-8");
    Metadata metadata = new Metadata();
    parser.parse(is, handler, metadata);
  }
  
}