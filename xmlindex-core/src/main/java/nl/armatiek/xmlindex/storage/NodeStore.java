package nl.armatiek.xmlindex.storage;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

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
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sf.saxon.s9api.SaxonApiException;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.PluggableIndex;
import nl.armatiek.xmlindex.conf.TypedValueDef;
import nl.armatiek.xmlindex.conf.VirtualAttributeDef;
import nl.armatiek.xmlindex.lucene.codec.XMLIndexCodec;

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
    // indexWriter.forceMergeDeletes(true);
    logger.info("Index #entries: " + getNumberOfIndexDocs());
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
  
  /*
  public String createDirectory(String path) throws IOException {
    IndexSearcher searcher = indexSearcherManager.acquire();
    try {
      String[] levels = StringUtils.split(path, '/');     
      
      Document parentDir = null;
      int l;
      for (l=levels.length-1; l>=0; l--) {
        String p = StringUtils.join(ArrayUtils.subarray(levels, 0, l), '/');
        Query query = SortedDocValuesField.newExactQuery(Definitions.FIELDNAME_PATH, new BytesRef(p));
        TopDocs docs = searcher.search(new TermQuery(new Term(Definitions.FIELDNAME_PATH, p)), 1);
        if (docs.totalHits > 0) {
          parentDir = searcher.doc(docs.scoreDocs[0].doc);
          break;
        }
      }
      
      
      
      
      new SortedDocValuesField(Definitions.FIELDNAME_PATH, new BytesRef(p)).newExactQuery(field, value)
      
      TopDocs docs = searcher.search(new TermQuery(new Term(Definitions.FIELDNAME_PATH, path)), 1);
      if (docs.totalHits > 0)
        return searcher.doc(docs.scoreDocs[0].doc).get(Definitions.FIELDNAME_DIRID);
      String dirId = UUID.randomUUID().toString();
      Document doc = new Document();
      doc.add(new StringField(Definitions.FIELDNAME_DIRID, dirId, Store.YES));
      doc.add(new StringField(Definitions.FIELDNAME_PARENTDIRID, dirId, Store.YES));
      return dirId;
    } finally {
      indexSearcherManager.release(searcher);
    }
  }
  */
  
  public void addDocument(String uri, InputStream is, String systemId) 
      throws ParserConfigurationException, SAXException, SaxonApiException, IOException {
    synchronized (index) {
      long nodeCounter = documentIndexer.getNodeCounter();
      try {     
        removeDocument(uri);
        documentIndexer.index(uri, is, systemId);
        storeNodeCounter(documentIndexer.getNodeCounter());
      } catch (Exception e) {
        removeDocument(uri);
        documentIndexer.setNodeCounter(nodeCounter);
        throw e;
      }
    }
  }
  
  public void addDocument(String uri, org.w3c.dom.Document doc) throws SaxonApiException, IOException {
    synchronized (index) {
      long nodeCounter = documentIndexer.getNodeCounter();
      try {     
        removeDocument(uri);
        documentIndexer.index(uri, doc);
        storeNodeCounter(documentIndexer.getNodeCounter());
      } catch (Exception e) {
        removeDocument(uri);
        documentIndexer.setNodeCounter(nodeCounter);
        throw e;
      }
    }
  }
  
  public void addDocuments(Path path, int maxDepth, String pattern) throws IOException {
    pattern = (pattern == null) ? "(.*?)" : pattern;
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
    Files.walk(path, maxDepth).filter(Files::isRegularFile).filter(matcher::matches).forEach(new Consumer<Path>() {
      public void accept(Path t) {
        try {
          InputStream is = new BufferedInputStream(new FileInputStream(t.toFile()));
          try {
            addDocument(path.relativize(t).toString(), is, t.toAbsolutePath().toString());
          } finally {
            is.close();
          }
        } catch (Exception e) {
          logger.error("Error adding document \"" + t.toString() + "\"", e);
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
    documentIndexer.reindexNode(session, doc, baseUriMap);
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
  
  public List<TypedValueDef> getTypedValueDefs() throws IOException {
    ArrayList<TypedValueDef> defs = new ArrayList<TypedValueDef>();
    IndexSearcher searcher = aquireIndexSearcher();
    try {
      TermQuery query = new TermQuery(new Term(Definitions.FIELDNAME_INDEXINFO, Definitions.FIELDVALUE_TYPEDVALDEF));
      TopDocs docs = searcher.search(query, 1024);
      for (ScoreDoc scoreDoc : docs.scoreDocs) {
        try {
          Document doc = searcher.doc(scoreDoc.doc);
          defs.add(new TypedValueDef(index, doc));
        } catch (Exception e) {
          logger.error("Error reading typed value definition", e);
        }
      }
    } finally {
      releaseIndexSearcher(searcher);
    }
    return defs;
  }
  
  public List<VirtualAttributeDef> getVirtualAttributeDefs(Path analyzerConfigPath) throws IOException {
    ArrayList<VirtualAttributeDef> defs = new ArrayList<VirtualAttributeDef>();
    IndexSearcher searcher = aquireIndexSearcher();
    try {
      TermQuery query = new TermQuery(new Term(Definitions.FIELDNAME_INDEXINFO, Definitions.FIELDVALUE_VIRTATTRDEF));
      TopDocs docs = searcher.search(query, 1024);
      for (ScoreDoc scoreDoc : docs.scoreDocs) {
        try {
          Document doc = searcher.doc(scoreDoc.doc);
          defs.add(new VirtualAttributeDef(index, doc, analyzerConfigPath));
        } catch (Exception e) {
          logger.error("Error reading virtual attribute definition", e);
        }
      }
    } finally {
      releaseIndexSearcher(searcher);
    }
    return defs;
  }
  
  public List<PluggableIndex> getPluggableIndexes() throws Exception {
    ArrayList<PluggableIndex> indexes = new ArrayList<PluggableIndex>();
    IndexSearcher searcher = aquireIndexSearcher();
    try {
      TermQuery query = new TermQuery(new Term(Definitions.FIELDNAME_INDEXINFO, Definitions.FIELDVALUE_PLUGGABLEINDEX));
      TopDocs docs = searcher.search(query, 1024);
      for (ScoreDoc scoreDoc : docs.scoreDocs) {
        try {
          Document doc = searcher.doc(scoreDoc.doc);
          indexes.add(PluggableIndex.fromDocument(index, doc));
        } catch (Exception e) {
          logger.error("Error reading pluggable index definition", e);
        }
      }
    } finally {
      releaseIndexSearcher(searcher);
    }
    return indexes;
  }
  
}