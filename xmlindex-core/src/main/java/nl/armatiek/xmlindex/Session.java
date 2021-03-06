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

package nl.armatiek.xmlindex;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.stax.XMLStreamWriterDestination;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.ObjectValue;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.TypedValueDef;
import nl.armatiek.xmlindex.conf.VirtualAttributeDef;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.node.DocumentElement;
import nl.armatiek.xmlindex.node.IndexRootElement;
import nl.armatiek.xmlindex.plugins.convertor.FileConvertor;
import nl.armatiek.xmlindex.plugins.index.PluggableIndex;
import nl.armatiek.xmlindex.saxon.XMLIndexURIResolver;
import nl.armatiek.xmlindex.saxon.optim.XMLIndexOptimizer;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexTreeInfo;
import nl.armatiek.xmlindex.storage.ReindexCollector;

public class Session {

  private static final Logger logger = LoggerFactory.getLogger(Session.class);
  
  private static final Map<String, XsltExecutable> xsltExecCache = new ConcurrentHashMap<String, XsltExecutable>();
  private static final Map<String, XQueryExecutable> xqueryExecCache = new ConcurrentHashMap<String, XQueryExecutable>();
  
  private final XMLIndex index;
  private final Configuration conf;
  private IndexSearcher indexSearcher;
  private final XMLIndexTreeInfo treeInfo;
  private boolean isOpen;
  private final boolean developmentMode; 
  private final Map<QName, XdmValue> params = new HashMap<QName, XdmValue>();
  
  public Session(XMLIndex index) {
    this.index = index;
    this.developmentMode = index.getConfiguration().getDevelopmentMode();
    this.conf = index.getSaxonConfiguration();
    this.treeInfo = new XMLIndexTreeInfo(this);
    this.params.put(Definitions.PARAM_SESSION_QN, XdmValue.wrap(new ObjectValue<Session>(this)));
  }
  
  public void open() throws IOException {
    if (isOpen)
      throw new XMLIndexException("Session is already open");
    indexSearcher = index.getNodeStore().aquireIndexSearcher();
    isOpen = true;
  }
  
  public void close() throws IOException {
    if (!isOpen)
      return;
    index.getNodeStore().releaseIndexSearcher(indexSearcher);
    indexSearcher = null;
    isOpen = false;
  }
  
  public boolean isOpen() {
    return isOpen;
  }
  
  public XMLIndexTreeInfo getTreeInfo() {
    return treeInfo;
  }
  
  public XMLIndex getIndex() {
    return index;
  }
  
  public Configuration getConfiguration() {
    return conf;
  }
  
  public IndexSearcher getIndexSearcher() throws IOException {
    checkOpen();
    return indexSearcher;
  }
  
  public void transform(Source stylesheet, Destination dest, Map<QName, XdmValue> params, ErrorListener errorListener, 
      MessageListener messageListener, String cacheKey, boolean explain) throws SaxonApiException {
    checkOpen();
    XsltExecutable exec = null;
    if (!developmentMode && cacheKey != null)
      exec = xsltExecCache.get(cacheKey);
    if (exec == null) {
      XsltCompiler comp = index.getSaxonProcessor().newXsltCompiler();
      if (errorListener != null)
        comp.setErrorListener(errorListener);  
      exec = comp.compile(stylesheet);
      if (!developmentMode)
        xsltExecCache.put(cacheKey, exec);
    }
    if (explain) {
      exec.explain(dest);
      return;
    }
    
    Xslt30Transformer transformer = exec.load30();
    Map<QName, XdmValue> combinedParams;
    if (params == null)
      combinedParams = this.params;
    else {
      combinedParams = params;
      combinedParams.putAll(this.params);
    }
    transformer.setStylesheetParameters(combinedParams);
    transformer.setURIResolver(new XMLIndexURIResolver(this));
    if (errorListener != null)
     transformer.setErrorListener(errorListener);
    if (messageListener != null)
      transformer.setMessageListener(messageListener);
    transformer.applyTemplates(treeInfo.getRootNode(), dest);
  }
  
  public void transform(Source stylesheet, Destination dest, Map<QName, XdmValue> params, ErrorListener errorListener, 
      MessageListener messageListener, String cacheKey) throws SaxonApiException {
    transform(stylesheet, dest, params, errorListener, messageListener, cacheKey, false);
  }
    
  public void query(Reader xquery, URI baseURI, Destination dest, Map<QName, XdmValue> params, ErrorListener errorListener, 
      MessageListener messageListener, String cacheKey, boolean explain) throws IOException, SaxonApiException {
    checkOpen();
    XQueryExecutable exec = null;
    if (!developmentMode && cacheKey != null)
      exec = xqueryExecCache.get(cacheKey);
    if (exec == null) {
      XQueryCompiler comp = index.getSaxonProcessor().newXQueryCompiler();
      if (baseURI != null)
        comp.setBaseURI(baseURI);
      if (errorListener != null)
        comp.setErrorListener(errorListener);
      exec = comp.compile(xquery);
      XMLIndexOptimizer.optimize(exec.getUnderlyingCompiledQuery().getExpression());
      if (!developmentMode)
        xqueryExecCache.put(cacheKey, exec);
    }
    if (explain) {
      exec.explain(dest);
      return;
    }
    XQueryEvaluator evaluator = exec.load();
    Map<QName, XdmValue> combinedParams;
    if (params == null)
      combinedParams = this.params;
    else {
      combinedParams = params;
      combinedParams.putAll(this.params);
    }
    for (Map.Entry<QName, XdmValue> entry : combinedParams.entrySet())
      evaluator.setExternalVariable(entry.getKey(), entry.getValue());
    evaluator.setURIResolver(new XMLIndexURIResolver(this));
    if (errorListener != null)
      evaluator.setErrorListener(errorListener);
    evaluator.setContextItem(new XdmNode(treeInfo.getRootNode()));
    evaluator.run(dest);
  }
  
  public void query(Reader xquery, URI baseURI, Destination dest, Map<QName, XdmValue> params, ErrorListener errorListener, 
      MessageListener messageListener, String cacheKey) throws IOException, SaxonApiException {
    query(xquery, baseURI, dest, params, errorListener, messageListener, cacheKey, false);
  }
  
  public void query(File queryFile, Destination dest, Map<QName, XdmValue> params, ErrorListener errorListener, 
      MessageListener messageListener, String cacheKey, boolean explain) throws IOException, SaxonApiException {
    Reader reader = new InputStreamReader(new FileInputStream(queryFile), StandardCharsets.UTF_8);
    try {
      query(reader, queryFile.toURI(), dest, params, errorListener, messageListener, cacheKey, explain);
    } finally {
      reader.close();
    }
  }
  
  public void query(File queryFile, Destination dest, Map<QName, XdmValue> params, ErrorListener errorListener, 
      MessageListener messageListener, String cacheKey) throws IOException, SaxonApiException {
    query(queryFile, dest, params, errorListener, messageListener, cacheKey, false);
  }
  
  public org.w3c.dom.Document getDOMDocument() {
    return (org.w3c.dom.Document) NodeOverNodeInfo.wrap(treeInfo.getRootNode());
  }
  
  /* NodeStore operations: */
  public void addDocument(String uri, InputStream is, String systemId, Map<String, Object> params) throws Exception {
    checkOpen();
    index.addDocument(uri, is, systemId, params);
  }
  
  public void addDocument(String uri, File xmlFile, Map<String, Object> params) throws Exception {
    checkOpen();
    InputStream is = new BufferedInputStream(new FileInputStream(xmlFile));
    try {
      addDocument(uri, is, xmlFile.getAbsolutePath(), params);
    } finally {
      is.close();
    }
  }
  
  public void addDocument(String uri, XdmNode doc, Map<String, Object> params) throws Exception {
    checkOpen();
    index.addDocument(uri, doc, params);
  }
  
  public void addDocuments(Path path, int maxDepth, Map<String, FileConvertor> fileSpecs, 
      boolean addDirectories, Map<String, Object> params) throws Exception {
    checkOpen();
    index.addDocuments(path, maxDepth, fileSpecs, addDirectories, params);
  }
  
  public void removeDocument(String uri) throws IOException {
    checkOpen();
    logger.info("Removing document \"" + uri + "\" ...");   
    index.removeDocument(uri);
  }
  
  private int getDocumentIdByUri(String uri) throws IOException {
    Query documentQuery = new TermQuery(new Term(Definitions.FIELDNAME_URI, uri));
    TopDocs topDocs = indexSearcher.search(documentQuery, 1);
    if (topDocs.totalHits == 0)
      return -1;
    return topDocs.scoreDocs[0].doc;
  }
  
  private Document getDocumentDocByUri(String uri) throws IOException {
    int docId = getDocumentIdByUri(uri);
    if (docId == -1)
      return null;    
    return indexSearcher.doc(docId);      
  }
  
  public XMLIndexNodeInfo getDocument(String uri) throws IOException {
    Document doc = getDocumentDocByUri(uri);
    if (doc == null)
      return null;
    DocumentElement elem = new DocumentElement(doc, this);
    return new XMLIndexNodeInfo(this, elem);
  }
  
  public AxisIterator getAllDocuments() {
    checkOpen();
    XMLIndexNodeInfo rootElem = new XMLIndexNodeInfo(this, new IndexRootElement());
    return rootElem.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT);
  }
  
  public XMLIndexNodeInfo getOwnerDocument(XMLIndexNodeInfo node) throws IOException {
    checkOpen();
    if (node == null)
      return null;
    return node.getOwnerDocument();
  }
  
  public String getDocumentURI(XMLIndexNodeInfo node) throws IOException {
    checkOpen();
    if (node == null)
      return null;
    return node.getDocumentURI();
  }
  
  public boolean documentExists(String uri) throws IOException {
    checkOpen();
    return getDocument(uri) != null;
  }
  
  public void commit() throws IOException {
    commit(false);
  }
  
  public void commit(boolean newSearcher) throws IOException {
    checkOpen();
    index.getNodeStore().commit(newSearcher);
    if (newSearcher) {
      synchronized(indexSearcher) {
        index.getNodeStore().releaseIndexSearcher(indexSearcher);
        indexSearcher = index.getNodeStore().aquireIndexSearcher();
      }
    }
  }
  
  /* NameStore operations: */
  public StructuredQName getStructuredQName(int nameCode, int prefixCode) {
    checkOpen();
    return index.getStructuredQName(nameCode, prefixCode);
  }
  
  public StructuredQName getStructuredQName(int nameCode) {
    checkOpen();
    return index.getStructuredQName(nameCode);
  }
  
  public int getNameCode(String namespaceUri, String localPart) {
    checkOpen();
    return index.getNameCode(namespaceUri, localPart);
  }
  
  public int putName(String uri, String localName) throws IOException {
    checkOpen();
    return index.putName(uri, localName);
  }
  
  private void checkOpen() {
    if (!isOpen)
      throw new XMLIndexException("Session is closed");
  }
  
  /* Reindex operations */
  public void reindexTypedValueDef(TypedValueDef def) throws IOException {
    logger.info("Reindexing typed value definition with node type \"" + def.getNodeType() + "\", qualified name \"" + def.getNodeName().getClarkName() + "\" and item type \"" + def.getItemType() + "\" ...");
    String fieldName = def.getNodeType() + "_" + index.getNameCode(def.getNamespaceUri(), def.getLocalPart());
    TermQuery query = new TermQuery(new Term(Definitions.FIELDNAME_FIELDNAMES, fieldName));
    indexSearcher.search(query, new ReindexCollector(this, index, def));
    commit(true);
    logger.info("Finished reindexing typed value definition with node type \"" + def.getNodeType() + "\", qualified name \"" + def.getNodeName().getClarkName() + "\" and item type \"" + def.getItemType() + "\"");
  }
  
  public void reindexVirtualAttributeDef(VirtualAttributeDef def) throws IOException {
    logger.info("Reindexing virtual attribute definition \"" + def.getName() + "\" ...");
    try {
      StringBuilder query = new StringBuilder();
      query.append("declare namespace func='" + Definitions.NAMESPACE_EXT_FUNCTIONS + "';\n");
      for (NamespaceBinding b : def.getNamespaceBindings())
        query.append("declare namespace ").append(b.getPrefix()).append('=').append("'").append(b.getURI()).append("';\n");
      String selector = def.getPattern();
      String[] clauses = StringUtils.split(selector, "|"); // TODO: check on | within filters
      ArrayList<String> fors = new ArrayList<String>();
      for (String clause : clauses) {
        clause = clause.trim();
        String prefix = "";
        if (selector.startsWith("//")) {
        } else if (selector.startsWith("/")) {
          prefix = "/*";
        } else {
          prefix = "//";
        }
        fors.add("for $a in " + prefix + clause + " return func:reindex-node($a)\n");
      }
      query.append('(').append(StringUtils.join(clauses, ",")).append(')');
      XQueryCompiler comp = index.getSaxonProcessor().newXQueryCompiler();
      XQueryExecutable exec = comp.compile(query.toString());
      XMLIndexOptimizer.optimize(exec.getUnderlyingCompiledQuery().getExpression());
      XQueryEvaluator evaluator = exec.load();
      Map<Long, String> baseUriMap = new HashMap<Long, String>();
      evaluator.setExternalVariable(Definitions.PARAM_BUM_QN, XdmValue.wrap(new ObjectValue<Map<Long, String>>(baseUriMap)));
      evaluator.run(new XMLStreamWriterDestination(XMLOutputFactory.newFactory().createXMLStreamWriter(new NullOutputStream()))); 
    } catch (Exception e) {
      throw new IOException("Error reindexing virtual attribute definition \"" + def.getName() + "\"");
    }
    commit(true);
    logger.info("Finished reindexing virtual attribute definition \"" + def.getName() + "\"");
  }
  
  public void reindexPluggableIndex(PluggableIndex pluggableIndex) throws IOException {
    logger.info("Reindexing pluggable index \"" + pluggableIndex.getClass().getName() + "\" ...");
    TermQuery query = new TermQuery(new Term(Definitions.FIELDNAME_TYPE, Integer.toString(Type.ELEMENT)));
    indexSearcher.search(query, new ReindexCollector(this, index, pluggableIndex));
    commit(true);
    logger.info("Finished reindexing pluggable index \"" + pluggableIndex.getClass().getName() + "\"");
  }
  
}