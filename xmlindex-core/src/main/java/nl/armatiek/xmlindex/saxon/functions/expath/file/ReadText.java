package nl.armatiek.xmlindex.saxon.functions.expath.file;

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

import java.io.File;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.commons.io.FileUtils;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.saxon.functions.expath.file.error.FileException;

/**
 * XPath extension function that returns the content of a file in its string representation.
 * 
 * @author Maarten Kroon
 * @see <a href="http://expath.org/spec/file">EXPath File Module</a>
 */
public class ReadText extends EXPathFileExtensionFunctionDefinition {

  private static final StructuredQName qName = 
      new StructuredQName("", NAMESPACEURI_EXPATH_FILE, "read-text");

  @Override
  public StructuredQName getFunctionQName() {
    return qName;
  }

  @Override
  public int getMinimumNumberOfArguments() {
    return 1;
  }

  @Override
  public int getMaximumNumberOfArguments() {
    return 2;
  }

  @Override
  public SequenceType[] getArgumentTypes() {    
    return new SequenceType[] { SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING };
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {    
    return SequenceType.SINGLE_STRING;
  }
  
  @Override
  public boolean hasSideEffects() {    
    return false;
  }

  @Override
  public ExtensionFunctionCall makeCallExpression() {    
    return new ReadTextCall();
  }
  
  private static class ReadTextCall extends FileExtensionFunctionCall {
        
    @Override
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {      
      try {                        
        File file = getFile(((StringValue) arguments[0].head()).getStringValue());
        if (!file.exists()) {
          throw new FileException(String.format("File \"%s\" does not exist", 
              file.getAbsolutePath()), FileException.ERROR_PATH_NOT_EXIST);
        }
        if (file.isDirectory()) {
          throw new FileException(String.format("Path \"%s\" points to a directory", 
              file.getAbsolutePath()), FileException.ERROR_PATH_IS_DIRECTORY);
        }        
        String encoding = "UTF-8";
        if (arguments.length > 1) {
          encoding = ((StringValue) arguments[1].head()).getStringValue();                   
        }        
        String value;
        try {
          value = FileUtils.readFileToString(file, encoding);
        } catch (UnsupportedCharsetException ece) {
          throw new FileException(String.format("Encoding \"%s\" is invalid or not supported", 
              encoding), FileException.ERROR_UNKNOWN_ENCODING);
        }                
        return new StringValue(value);
      } catch (FileException fe) {
        throw fe;
      } catch (Exception e) {
        throw new FileException("Other file error", e, FileException.ERROR_IO);
      }
    } 
  }
}