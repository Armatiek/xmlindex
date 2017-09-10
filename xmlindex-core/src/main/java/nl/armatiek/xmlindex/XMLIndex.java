package nl.armatiek.xmlindex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.xml.validation.Schema;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.ItemTypeFactory;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;
import nl.armatiek.xmlindex.conf.Definitions;
import nl.armatiek.xmlindex.conf.IndexConfig;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.lucene.codec.XMLIndexStoredFieldsFormat.Mode;
import nl.armatiek.xmlindex.plugins.convertor.FileConvertor;
import nl.armatiek.xmlindex.saxon.conf.XMLIndexConfiguration;
import nl.armatiek.xmlindex.saxon.conf.XMLIndexInitializer;
import nl.armatiek.xmlindex.storage.NameStore;
import nl.armatiek.xmlindex.storage.NodeStore;

public class XMLIndex {
  
  private static final Logger logger = LoggerFactory.getLogger(XMLIndex.class);
  
  public static final Mode DEFAULT_INDEX_COMPRESSION = Mode.NO_COMPRESSION;
  public static final int DEFAULT_MAX_TERM_LENGTH    = 1024;
  
  private final Stack<Session> sessionPool = new Stack<Session>();
  
  private final Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
  private final PerFieldAnalyzerWrapper perFieldWrapper = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerPerField);
  
  private volatile IndexConfig config;
  private final Schema configSchema;
  private final String indexName;
  private final Path indexPath;
  private final Path configPath;
  private final Processor saxonProcessor;
  private final XMLIndexInitializer initializer;
  private final Configuration saxonConfig;
  private final ItemTypeFactory itemTypeFactory;
  private final NodeStore nodeStore;
  private final NameStore nameStore;
  private int maxTermLength = 1024;
  private Mode indexCompression = Mode.NO_COMPRESSION;
  private boolean isOpen;
  
  public XMLIndex(String indexName, Path indexPath, int maxTermLength, 
      Mode indexCompression, Schema configSchema) throws IOException {
    this.indexName = indexName;
    this.indexPath = indexPath;
    this.maxTermLength = maxTermLength;
    this.indexCompression = indexCompression;
    this.configSchema = configSchema;
    File indexDir = indexPath.toFile();
    if (!indexDir.isDirectory() && !indexDir.mkdirs())
      throw new XMLIndexException("Unable to create index directory \"" + indexDir.getAbsolutePath() + "\"");
    this.configPath = indexPath.resolve(Definitions.FOLDERNAME_CONF);  
    File configDir = configPath.toFile();
    if (!configDir.isDirectory() && !configDir.mkdirs())
      throw new XMLIndexException("Unable to create configuration directory \"" + configDir.getAbsolutePath() + "\"");
      
    processConfigProperties();
    saxonConfig = new XMLIndexConfiguration(this);
    initializer = new XMLIndexInitializer();
    initializer.initializeConfiguration(saxonConfig);
    saxonProcessor = new Processor(saxonConfig);
    itemTypeFactory = new ItemTypeFactory(saxonProcessor);
    nameStore = new NameStore(this);
    nodeStore = new NodeStore(this);
  }
  
  public XMLIndex(String indexName, Path indexPath, int maxTermLength, 
      Mode indexCompression) throws IOException {
    this(indexName, indexPath, maxTermLength, indexCompression, null); 
  }
  
  public XMLIndex(String indexName, Path indexPath, Schema configSchema) throws IOException {
    this(indexName, indexPath, DEFAULT_MAX_TERM_LENGTH, DEFAULT_INDEX_COMPRESSION, configSchema); 
  }
  
  public XMLIndex(String indexName, Path indexPath) throws IOException {
    this(indexName, indexPath, DEFAULT_MAX_TERM_LENGTH, DEFAULT_INDEX_COMPRESSION); 
  }
  
  public synchronized void reloadConfig() {
    IndexConfig newConfig = null;
    try {
      newConfig = new IndexConfig(this, analyzerPerField, configSchema);
    } catch (Exception e) {
      logger.error("Error reloading configuration of index \"" + indexName + "\"", e);
      throw e;
    }
    this.config = newConfig;
  }
  
  public boolean isOpen() {
    return isOpen;
  }
  
  public void open() throws IOException {
    if (isOpen)
      throw new XMLIndexException("Index already opened");
    logger.info("Opening index \"" + indexName + "\" ...");
    try {
      config = new IndexConfig(this, analyzerPerField, configSchema);
      nodeStore.open();
      nameStore.open();
      isOpen = true;
      logger.info("Index \"" + indexName + "\" opened");
    } catch (Exception e) {
      logger.error("Error opening index \"" + indexName + "\"", e);
      throw e;
    }
  }
  
  public void close() throws IOException {
    if (!isOpen)
      return;
    logger.info("Closing index \"" + indexName + "\" ...");
    try {
      nodeStore.close();
      nameStore.close();
      isOpen = false;
      logger.info("Index \"" + indexName + "\" closed");
    } catch (Exception e) {
      logger.error("Error closing index \"" + indexName + "\"", e);
      throw e;
    }
  }
    
  public IndexConfig getConfiguration() {
    return config;
  }
  
  public Processor getSaxonProcessor() {
    return this.saxonProcessor;
  }
    
  public Configuration getSaxonConfiguration() {
    return saxonConfig;
  }
  
  public String getIndexName() {
    return indexName;
  }
  
  public Path getIndexPath() {
    return indexPath;
  }
  
  public Path getConfigPath() {
    return configPath;
  }
  
  public ItemTypeFactory getItemTypeFactory() {
    return itemTypeFactory;
  }
  
  public NodeStore getNodeStore() {
    return nodeStore;
  }
  
  public NameStore getNameStore() {
    return nameStore;
  }
  
  public Analyzer getAnalyzer() {
    return perFieldWrapper;
  }
  
  public Mode getIndexCompression() {
    return indexCompression;
  }
  
  public int getMaxTermLength() {
    return maxTermLength;
  }
  
  public synchronized Session aquireSession() throws IOException {
    checkOpen();
    Session session;
    if (sessionPool.empty())
      session = new Session(this);
    else
      session = sessionPool.pop();
    session.open();
    return session;
  }
  
  public synchronized void returnSession(Session session) throws IOException {
    checkOpen();
    if (session.isOpen())
      session.close();
    sessionPool.push(session);
  }
  
  /* NodeStore operations */
  public void addDocument(String uri, InputStream is, String systemId, Map<String, Object> params) throws Exception {
    checkOpen();
    nodeStore.addDocument(uri, is, systemId, params);
  }
  
  public void addDocument(String uri, XdmNode doc, Map<String, Object> params) throws Exception {
    checkOpen();
    nodeStore.addDocument(uri, doc, params);
  }
  
  public void addDocuments(Path path, int maxDepth, Map<String, FileConvertor> fileSpecs, 
      boolean addDirectories, Map<String, Object> params) throws Exception {
    checkOpen();
    nodeStore.addDocuments(path, maxDepth, fileSpecs, addDirectories, params);
  }
  
  public void removeDocument(String uri) throws IOException {
    checkOpen();
    nodeStore.removeDocument(uri);
  }
    
  /* NameStore operations */
  public StructuredQName getStructuredQName(int nameCode, int prefixCode) {
    checkOpen();
    return nameStore.getStructuredQName(nameCode, prefixCode);
  }
  
  public StructuredQName getStructuredQName(int nameCode) {
    checkOpen();
    return nameStore.getStructuredQName(nameCode);
  }
  
  public int getNameCode(String namespaceUri, String localPart) {
    checkOpen();
    return nameStore.getNameCode(namespaceUri, localPart);
  }
  
  public int putName(String uri, String localName) throws IOException {
    checkOpen();
    return nameStore.putName(uri, localName);
  }
  
  public int putPrefix(String prefix) throws IOException {
    checkOpen();
    return nameStore.putPrefix(prefix);
  }
  
  private void checkOpen() {
    if (!isOpen)
      throw new XMLIndexException("Index is closed");
  }
  
  private void processConfigProperties() throws IOException {
    File propsFile = new File(configPath.toFile(), Definitions.FILENAME_PROPERTIES);
    Properties props = new Properties();
    if (propsFile.exists()) {
      InputStreamReader reader = new InputStreamReader(new FileInputStream(propsFile), StandardCharsets.UTF_8);
      try {
        props.load(reader);
        indexCompression = Mode.valueOf(props.getProperty(Definitions.PROPNAME_INDEX_COMPRESSION, "no_compression").toUpperCase());
        maxTermLength = Integer.parseInt(props.getProperty(Definitions.PROPNAME_MAX_TERM_LENGTH, "1024"));
      } finally {
        reader.close();
      }
    } else {
      props.setProperty(Definitions.PROPNAME_INDEX_COMPRESSION, indexCompression.toString().toLowerCase());
      props.setProperty(Definitions.PROPNAME_MAX_TERM_LENGTH, Integer.toString(maxTermLength));
      OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(propsFile), StandardCharsets.UTF_8);
      try {
        props.store(writer, "DO NOT EDIT");
      } finally {
        writer.close();
      }
    }
  }

}