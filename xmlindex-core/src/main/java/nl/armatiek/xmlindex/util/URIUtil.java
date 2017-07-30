package nl.armatiek.xmlindex.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class URIUtil {
  
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