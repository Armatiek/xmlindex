package nl.armatiek.xmlindex.utils;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.DirectoryFileFilter;

import nl.armatiek.xmlindex.error.XMLIndexException;

/**
 * Miscellaneous XSLWeb specific helper methods.
 *
 * @author Maarten
 */
public class XMLIndexWebUtils {
  
  private static Pattern variablesPattern = Pattern.compile("\\$\\{(.+?)\\}");
  
  public static String resolveProperties(String sourceString, Properties props) {
    if (sourceString == null) {
      return null;
    }
    Matcher m = variablesPattern.matcher(sourceString);
    StringBuffer result = new StringBuffer();
    while (m.find()) {
      String variable = m.group(1);
      String value = props.getProperty(variable);
      if (value == null) {
        throw new XMLIndexException(String.format("No value specified for variable \"%s\"", variable));
      }
      String resolved = resolveProperties(value.toString(), props);
      resolved = resolved.replaceAll("([\\\\\\$])", "\\\\$1");
      m.appendReplacement(result, resolved);
    }
    m.appendTail(result);
    return result.toString();
  }
  
  public static Properties readProperties(File propsFile) throws IOException {    
    if (!propsFile.isFile()) {
      throw new FileNotFoundException(String.format("Properties file \"%s\" not found", propsFile.getAbsolutePath()));
    }
    Properties props = new Properties();
    InputStream is = new BufferedInputStream(new FileInputStream(propsFile));
    try {
      props.load(is);
    } finally {
      is.close();
    } 
    return props;
  }
  
  public static boolean hasSubDirectories(File file) {
    return file.listFiles((FileFilter) DirectoryFileFilter.INSTANCE).length > 0;
  }
  
  public static String encodeForURI(String input) {
    StringBuilder resultStr = new StringBuilder();
    for (char ch : input.toCharArray()) {
      if (isUnsafe(ch)) {
        resultStr.append('%');
        resultStr.append(toHex(ch / 16));
        resultStr.append(toHex(ch % 16));
      } else {
        resultStr.append(ch);
      }
    }
    return resultStr.toString();
  }

  private static char toHex(int ch) {
    return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
  }

  private static boolean isUnsafe(char ch) {
    if (ch > 128 || ch < 0)
      return true;
    return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
  }
  
}