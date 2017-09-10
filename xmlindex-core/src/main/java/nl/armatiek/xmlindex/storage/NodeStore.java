package nl.armatiek.xmlindex.storage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.ReferenceManager.RefreshListener;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.lucene.codec.XMLIndexCodec;
import nl.armatiek.xmlindex.plugins.convertor.FileConvertor;

public class NodeStore {
  
  private static final Logger logger = LoggerFactory.getLogger(NodeStore.class);
  
  private static final double MAX_STALE_SEC = 1.0;
  private static final double MIN_STALE_SEC = 0.1;
  
  private final Term uniqueKeyNodeCounter = new Term(Definitions.FIELDNAME_INDEXINFO, Definitions.FIELDVALUE_NODECOUNTER);
  
  // private boolean isOpen;
  private final XMLIndex index;
  private IndexWriter indexWriter;
  private ReferenceManager<IndexSearcher> indexSearcherManager;
  private ControlledRealTimeReopenThread<IndexSearcher> indexReopenThread;
  private DocumentIndexer documentIndexer;
  
  public NodeStore(XMLIndex index) {
    this.index = index;
  }
  
  public void open() throws IOException {
    logger.info("Opening node store ...");
    Directory dir = FSDirectory.open(index.getIndexPath().resolve(Definitions.FOLDERNAME_NODESTORE));
    IndexWriterConfig config = new IndexWriterConfig(index.getAnalyzer());
    //Sort indexSort = new Sort(new SortField(Definitions.FIELDNAME_LEFT, SortField.Type.LONG));
    //config.setIndexSort(indexSort);
    config.setRAMBufferSizeMB(128);
    config.setCommitOnClose(true);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND); 
    Codec codec = new XMLIndexCodec(index.getIndexCompression());
    logger.info("Setting index compression to: " + index.getIndexCompression().name());
    config.setCodec(codec);
    this.indexWriter = new IndexWriter(dir, config);
    this.indexSearcherManager = new SearcherManager(this.indexWriter, false, false, null);
    if (logger.isDebugEnabled())
      this.indexSearcherManager.addListener(new RefreshListener() {
  
        @Override
        public void afterRefresh(boolean didRefresh) throws IOException {
          logger.debug("Refreshed index searcher");
        }
  
        @Override
        public void beforeRefresh() throws IOException { }
        
      });
    this.indexReopenThread = new ControlledRealTimeReopenThread<IndexSearcher>(this.indexWriter, 
        indexSearcherManager, MAX_STALE_SEC, MIN_STALE_SEC);
    this.indexReopenThread.setName("NRT Reopen Thread");
    this.indexReopenThread.setPriority(Math.min(Thread.currentThread().getPriority() + 2, Thread.MAX_PRIORITY));
    this.indexReopenThread.setDaemon(true);
    this.indexReopenThread.start();
    createRootNode();
    logger.info("Number of documents in index: " + getNumberOfIndexDocs());
    logger.info("Index has deletions: " + indexWriter.hasDeletions());
    long nodeCounter = getNodeCounter();   
    this.documentIndexer = new DocumentIndexer(index, (long) nodeCounter);
  }
  
  public void close() throws IOException {
    logger.info("Closing node store ...");
    this.indexReopenThread.interrupt();
    this.indexReopenThread.close(); 
    this.indexWriter.close();
    this.documentIndexer = null;
    logger.info("Node store closed");
  }
    
  public IndexSearcher aquireIndexSearcher() throws IOException {
    return indexSearcherManager.acquire();
  }
  
  public void releaseIndexSearcher(IndexSearcher searcher) throws IOException {
    indexSearcherManager.release(searcher);
  }
  
  public void addLuceneDocument(Document doc) throws IOException {
    indexWriter.addDocument(doc);
  }
  
  public void updateLuceneDocument(Term term, Document doc) throws IOException {
    indexWriter.updateDocument(term, doc);
  }
  
  public void deleteLuceneDocument(Term term) throws IOException {
    indexWriter.deleteDocuments(term);
  }
  
  public void deleteLuceneDocument(Query query) throws IOException {
    indexWriter.deleteDocuments(query);
  }
  
  public void addDocument(String uri, InputStream is, String systemId, Map<String, Object> params) throws Exception {
    synchronized (documentIndexer) {
      long nodeCounter = documentIndexer.getNodeCounter();
      try {     
        removeDocument(uri);
        documentIndexer.index(uri, is, systemId, params);
        storeNodeCounter(documentIndexer.getNodeCounter());
      } catch (Exception e) {
        removeDocument(uri);
        documentIndexer.setNodeCounter(nodeCounter);
        throw e;
      }
    }
  }
  
  public void addDocument(String uri, XdmNode doc, Map<String, Object> params) throws Exception {
    synchronized (documentIndexer) {
      long nodeCounter = documentIndexer.getNodeCounter();
      try {     
        removeDocument(uri);
        documentIndexer.index(uri, doc, params);
        storeNodeCounter(documentIndexer.getNodeCounter());
      } catch (Exception e) {
        removeDocument(uri);
        documentIndexer.setNodeCounter(nodeCounter);
        throw e;
      }
    }
  }
  
  public void addDocuments(Path path, int maxDepth, Map<String, FileConvertor> fileSpecs, 
      boolean addDirectories, Map<String, Object> params) throws IOException {
    
    Map<PathMatcher, FileConvertor> matchers = new HashMap<PathMatcher, FileConvertor>();
    Iterator<Entry<String, FileConvertor>> it = fileSpecs.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, FileConvertor> entry = (Entry<String, FileConvertor>) it.next();
      String pattern = entry.getKey();
      pattern = (pattern == null) ? "(.*?)" : pattern;
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
      matchers.put(matcher, entry.getValue());
    }
    
    Files.walk(path, maxDepth).forEach(new Consumer<Path>() {
      public void accept(Path filePath) {
        try {
          boolean isDirectory = Files.isDirectory(filePath);
          if (!addDirectories && isDirectory)
            return;
          
          Entry<PathMatcher, FileConvertor> matcher = null;
          Iterator<Entry<PathMatcher, FileConvertor>> it = matchers.entrySet().iterator();
          while (it.hasNext()) {
            Entry<PathMatcher, FileConvertor> currentMatcher = (Entry<PathMatcher, FileConvertor>) it.next();
            if (currentMatcher.getKey().matches(filePath)) {
              matcher = currentMatcher;
              break;
            }
          }
          
          if (matcher == null)
            return;
          
          Map<String, Object> pathParams = new HashMap<String, Object>();
          pathParams.put("path", filePath.normalize().toString());
          pathParams.put("parent-path", filePath.getParent().toString());
          String fileName = filePath.getFileName().toString();
          pathParams.put("name", fileName);
          String ext = !isDirectory ? StringUtils.substringAfterLast(fileName, ".") : null;
          if (ext != null)
            pathParams.put("extension", ext);
          BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
          pathParams.put("creation-time", attrs.creationTime());
          pathParams.put("last-modified-time", attrs.lastModifiedTime());
          pathParams.put("last-access-time", attrs.lastAccessTime());
          pathParams.put("size", attrs.size());
          pathParams.put("is-symbolic-link", attrs.isSymbolicLink());
          pathParams.put("is-file", !isDirectory);
          if (params != null)
            pathParams.putAll(params);
          
          if (isDirectory) {
            InputStream content = IOUtils.toInputStream("<dir:directory xmlns:dir=\"" + Definitions.NAMESPACE_DIRECTORY + "\"/>", StandardCharsets.UTF_8);
            addDocument(path.relativize(filePath).toString(), content , filePath.toAbsolutePath().toString(), pathParams);
          } else {
            InputStream is = new BufferedInputStream(new FileInputStream(filePath.toFile()));
            try {
              InputStream cis = null;
              FileConvertor convertor = matcher.getValue();
              if (convertor != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                convertor.convert(is, filePath.toString(), baos);
                cis = new ByteArrayInputStream(baos.toByteArray());
              }
              addDocument(path.relativize(filePath).toString(), (cis != null) ? cis : is, filePath.toAbsolutePath().toString(), pathParams);
            } finally {
              is.close();
            }
          }
        } catch (Exception e) {
          logger.error("Error adding document \"" + filePath.toString() + "\"", e);
        }
      }
    });
  }
  
  public void removeDocument(String uri) throws IOException {
    indexWriter.deleteDocuments(new Term(Definitions.FIELDNAME_BASEURI, uri));
  }
      
  public void commit() throws IOException {
    commit(false);
  }
  
  public void commit(boolean waitSearcher) throws IOException {
    if (waitSearcher) {
      logger.info("Committing changes and wait for new searcher ...");
      this.indexWriter.commit();
      this.indexSearcherManager.maybeRefreshBlocking();
    } else {
      logger.info("Committing changes ...");
      this.indexWriter.commit();
      this.indexSearcherManager.maybeRefresh();
    }
  }
  
  public void reindexNode(Session session, Document doc, Map<Long, String> baseUriMap) throws IOException, SaxonApiException {
    synchronized (documentIndexer) {
      documentIndexer.reindexNode(session, doc, baseUriMap);
    }
  }
  
  private void createRootNode() throws IOException {
    logger.info("Creating document and root nodes ...");
    IndexSearcher searcher = aquireIndexSearcher();
    try {
      TopDocs topDocs = searcher.search(LongPoint.newExactQuery(Definitions.FIELDNAME_LEFT, 0), 1);
      if (topDocs.totalHits == 0) {
        storeNodeCounter(2);
        
        /* Document node: */
        Document doc = new Document(); 
        doc.add(new StringField(Definitions.FIELDNAME_TYPE, Integer.toString(Node.DOCUMENT_NODE), Field.Store.YES));
        doc.add(new StoredField(Definitions.FIELDNAME_DEPTH, "0"));
        doc.add(new LongPoint(Definitions.FIELDNAME_LEFT, 0));
        doc.add(new NumericDocValuesField(Definitions.FIELDNAME_LEFT, 0));
        doc.add(new StoredField(Definitions.FIELDNAME_LEFT, 0));
        doc.add(new LongPoint(Definitions.FIELDNAME_RIGHT, Definitions.MAX_LONG));
        doc.add(new NumericDocValuesField(Definitions.FIELDNAME_RIGHT, Definitions.MAX_LONG));
        doc.add(new StoredField(Definitions.FIELDNAME_RIGHT, Definitions.MAX_LONG));
        doc.add(new LongPoint(Definitions.FIELDNAME_PARENT, -1));
        doc.add(new StoredField(Definitions.FIELDNAME_PARENT, -1));
        doc.add(new NumericDocValuesField(Definitions.FIELDNAME_PARENT, -1));
        addLuceneDocument(doc);
        
        /* Document element node: */
        doc = new Document(); 
        doc.add(new StringField(Definitions.FIELDNAME_TYPE, Integer.toString(Node.ELEMENT_NODE), Field.Store.YES));
        doc.add(new StoredField(Definitions.FIELDNAME_DEPTH, "1"));
        doc.add(new LongPoint(Definitions.FIELDNAME_LEFT, 1));
        doc.add(new NumericDocValuesField(Definitions.FIELDNAME_LEFT, 1));
        doc.add(new StoredField(Definitions.FIELDNAME_LEFT, 1));
        doc.add(new LongPoint(Definitions.FIELDNAME_RIGHT, Definitions.MAX_LONG-1));
        doc.add(new NumericDocValuesField(Definitions.FIELDNAME_RIGHT, Definitions.MAX_LONG-1));
        doc.add(new StoredField(Definitions.FIELDNAME_RIGHT, Definitions.MAX_LONG-1));
        doc.add(new LongPoint(Definitions.FIELDNAME_PARENT, 0));
        doc.add(new StoredField(Definitions.FIELDNAME_PARENT, 0));
        doc.add(new NumericDocValuesField(Definitions.FIELDNAME_PARENT, 0));
        doc.add(new StringField(Definitions.FIELDNAME_FIELDNAMES, "1_0", Field.Store.NO));
        doc.add(new StringField("1_0", "", Field.Store.YES));
        addLuceneDocument(doc);
        
        commit();
      }
    } finally {
      releaseIndexSearcher(searcher);
    }
    indexSearcherManager.maybeRefreshBlocking();
  }
  
  private long getNodeCounter() throws IOException {
    IndexSearcher searcher = aquireIndexSearcher();
    try {
      TopDocs topDocs = searcher.search(new TermQuery(uniqueKeyNodeCounter), 1);
      Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
      return ((StoredField) doc.getField(Definitions.FIELDNAME_NODECOUNTER)).numericValue().longValue();
    } finally {
      releaseIndexSearcher(searcher);
    }
  }
  
  private void storeNodeCounter(long counter) throws IOException {
    Document doc = new Document();
    doc.add(new StringField(Definitions.FIELDNAME_INDEXINFO, Definitions.FIELDVALUE_NODECOUNTER, Store.NO));
    doc.add(new StoredField(Definitions.FIELDNAME_NODECOUNTER, counter));
    doc.add(new NumericDocValuesField(Definitions.FIELDNAME_LEFT, Definitions.FIELVALUE_NODECOUNTER_LEFT));
    indexWriter.updateDocument(uniqueKeyNodeCounter, doc);
  }
  
  private int getNumberOfIndexDocs() throws IOException {
    IndexSearcher searcher = aquireIndexSearcher();
    try {
      return searcher.count(new MatchAllDocsQuery());
    } finally {
      releaseIndexSearcher(searcher);
    }
  }
  
}