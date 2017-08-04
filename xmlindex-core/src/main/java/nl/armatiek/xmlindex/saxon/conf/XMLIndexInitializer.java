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

package nl.armatiek.xmlindex.saxon.conf;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.TransformerException;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.Initializer;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Append;
import nl.armatiek.xmlindex.saxon.functions.expath.file.AppendBinary;
import nl.armatiek.xmlindex.saxon.functions.expath.file.AppendText;
import nl.armatiek.xmlindex.saxon.functions.expath.file.AppendTextLines;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Children;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Copy;
import nl.armatiek.xmlindex.saxon.functions.expath.file.CreateDir;
import nl.armatiek.xmlindex.saxon.functions.expath.file.CreateTempDir;
import nl.armatiek.xmlindex.saxon.functions.expath.file.CreateTempFile;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Delete;
import nl.armatiek.xmlindex.saxon.functions.expath.file.DirName;
import nl.armatiek.xmlindex.saxon.functions.expath.file.DirSeparator;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Exists;
import nl.armatiek.xmlindex.saxon.functions.expath.file.IsDir;
import nl.armatiek.xmlindex.saxon.functions.expath.file.IsFile;
import nl.armatiek.xmlindex.saxon.functions.expath.file.LastModified;
import nl.armatiek.xmlindex.saxon.functions.expath.file.LineSeparator;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Move;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Name;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Parent;
import nl.armatiek.xmlindex.saxon.functions.expath.file.PathSeparator;
import nl.armatiek.xmlindex.saxon.functions.expath.file.PathToNative;
import nl.armatiek.xmlindex.saxon.functions.expath.file.PathToURI;
import nl.armatiek.xmlindex.saxon.functions.expath.file.ReadBinary;
import nl.armatiek.xmlindex.saxon.functions.expath.file.ReadText;
import nl.armatiek.xmlindex.saxon.functions.expath.file.ReadTextLines;
import nl.armatiek.xmlindex.saxon.functions.expath.file.ResolvePath;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Size;
import nl.armatiek.xmlindex.saxon.functions.expath.file.TempDir;
import nl.armatiek.xmlindex.saxon.functions.expath.file.Write;
import nl.armatiek.xmlindex.saxon.functions.expath.file.WriteBinary;
import nl.armatiek.xmlindex.saxon.functions.expath.file.WriteText;
import nl.armatiek.xmlindex.saxon.functions.expath.file.WriteTextLines;
import nl.armatiek.xmlindex.saxon.functions.expath.httpclient.expath.pkg.saxon.EXPathFunctionDefinition;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.AddDocument;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.AddPluggableIndex;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.AddTypedValueDef;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.AddVirtualAttributeDef;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.Collection;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.Commit;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.CopyToMemory;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.Document;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.DocumentAvailable;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.DocumentURI;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.GetConfiguration;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.LocalVariableReferencer;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.OwnerDocument;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.PluggableIndexExists;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.RemoveDocument;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.RemovePluggableIndex;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.RemoveTypedValueDef;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.RemoveVirtualAttributeDef;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.TypedValueDefExists;
import nl.armatiek.xmlindex.saxon.functions.xmlindex.VirtualAttributeDefExists;

public class XMLIndexInitializer implements Initializer {
  
  private Set<String> functionClassNames = new HashSet<String>(); 

  @Override
  public void initialize(Configuration configuration) throws TransformerException {    
    configuration.setXIncludeAware(true);
        
    configuration.setConfigurationProperty(FeatureKeys.RECOVERY_POLICY_NAME, "recoverWithWarnings");
    configuration.setConfigurationProperty(FeatureKeys.SUPPRESS_XSLT_NAMESPACE_CHECK, Boolean.TRUE);
    
    /* XMLIndex */
    registerFunction(new LocalVariableReferencer(), configuration);
    registerFunction(new CopyToMemory(), configuration);
    
    registerFunction(new AddDocument(), configuration);
    registerFunction(new Collection(), configuration);
    registerFunction(new Commit(), configuration);
    registerFunction(new Document(), configuration);
    registerFunction(new DocumentAvailable(), configuration);
    registerFunction(new DocumentURI(), configuration);
    registerFunction(new OwnerDocument(), configuration);
    registerFunction(new RemoveDocument(), configuration);
    
    registerFunction(new AddPluggableIndex(), configuration);
    registerFunction(new AddTypedValueDef(), configuration);
    registerFunction(new AddVirtualAttributeDef(), configuration);
    registerFunction(new GetConfiguration(), configuration);
    registerFunction(new PluggableIndexExists(), configuration);
    registerFunction(new TypedValueDefExists(), configuration);
    registerFunction(new VirtualAttributeDefExists(), configuration);
    registerFunction(new RemovePluggableIndex(), configuration);
    registerFunction(new RemoveTypedValueDef(), configuration);
    registerFunction(new RemoveVirtualAttributeDef(), configuration);
    
    /* EXPath File: */
    registerFunction(new Append(), configuration);
    registerFunction(new AppendBinary(), configuration);
    registerFunction(new AppendText(), configuration);
    registerFunction(new AppendTextLines(), configuration);
    registerFunction(new Children(), configuration);
    registerFunction(new Copy(), configuration);
    registerFunction(new CreateDir(), configuration);
    registerFunction(new CreateTempDir(), configuration);
    registerFunction(new CreateTempFile(), configuration);
    registerFunction(new Delete(), configuration);
    registerFunction(new DirName(), configuration);
    registerFunction(new DirSeparator(), configuration);
    registerFunction(new Exists(), configuration);
    registerFunction(new IsDir(), configuration);
    registerFunction(new IsFile(), configuration);
    registerFunction(new LastModified(), configuration);
    registerFunction(new LineSeparator(), configuration);
    registerFunction(new nl.armatiek.xmlindex.saxon.functions.expath.file.List(), configuration);
    registerFunction(new Move(), configuration);
    registerFunction(new Name(), configuration);
    registerFunction(new Parent(), configuration);
    registerFunction(new PathSeparator(), configuration);
    registerFunction(new PathToNative(), configuration);
    registerFunction(new PathToURI(), configuration);
    registerFunction(new ReadBinary(), configuration);
    registerFunction(new ReadText(), configuration);
    registerFunction(new ReadTextLines(), configuration);
    registerFunction(new ResolvePath(), configuration);
    registerFunction(new Size(), configuration);
    registerFunction(new TempDir(), configuration);
    registerFunction(new Write(), configuration);
    registerFunction(new WriteBinary(), configuration);
    registerFunction(new WriteText(), configuration);
    registerFunction(new WriteTextLines(), configuration);
  }
  
  public void initializeConfiguration(Configuration configuration) throws IOException {
    try {
      initialize(configuration);
    } catch (TransformerException te) {
      throw new IOException("Error initializing Saxon configuration", te);
    }
  }
  
  private void registerFunction(ExtensionFunctionDefinition function, Configuration configuration) {              
    configuration.registerExtensionFunction(function);
    functionClassNames.add(function.getClass().getName());
    if (function instanceof EXPathFunctionDefinition) {
      ((EXPathFunctionDefinition) function).setConfiguration(configuration);
    }
  }
  
  public boolean isFunctionRegistered(String className) {
    return functionClassNames.contains(className);
  }
  
}