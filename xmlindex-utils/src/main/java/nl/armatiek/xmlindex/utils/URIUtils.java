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

package nl.armatiek.xmlindex.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class URIUtils {
  
  public static Map<String, List<String>> getQueryParams(URI uri) throws UnsupportedEncodingException {
    String query = uri.getRawQuery();
    if (query == null)
      return null;
    final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
    final String[] pairs = query.split("&");
    for (String pair : pairs) {
      final int idx = pair.indexOf("=");
      final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
      if (!query_pairs.containsKey(key)) {
        query_pairs.put(key, new LinkedList<String>());
      }
      final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
      query_pairs.get(key).add(value);
    }
    return query_pairs;
  }

}