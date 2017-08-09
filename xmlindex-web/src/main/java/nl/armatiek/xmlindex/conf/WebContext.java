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

package nl.armatiek.xmlindex.conf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.utils.XMLIndexWebUtils;

/**
 * 
 * 
 * @author Maarten
 */
public class WebContext {
  
  private static final Logger logger = LoggerFactory.getLogger(WebContext.class);
  
  private Map<String, XMLIndex> indexes = new ConcurrentHashMap<String, XMLIndex>();
  
  private static WebContext _instance;
  
  private ServletContext servletContext;
  private Properties properties;
  private boolean developmentMode;
  private boolean parserHardening;
  private boolean trustAllCerts;
  private File indexesDir;
  private String contextPath;
  private File webInfDir; 
  private File homeDir;
  private volatile boolean isOpen = false;
  
  private WebContext() { }
  
  /**
   * Returns the singleton Config instance.
   */
  public static synchronized WebContext getInstance() {
    if (_instance == null) {
      _instance = new WebContext();
    }
    return _instance;
  }

  public void open() throws Exception {
    logger.info("Opening XMLIndex Context ...");
    
    initHomeDir();      
    initProperties();
    
    isOpen = true;
    logger.info("XMLIndex Context opened.");
  }
  
  public void close() throws Exception {
    logger.info("Closing XMLIndex Context ...");
    
    isOpen = false;
   
    for (XMLIndex index : indexes.values()) {
      logger.info("Closing index \"" + index.getIndexName() + "\" ... ");
      index.close();
    }
     
    logger.info("XMLIndex Context closed.");
  }
  
  /**
   * Returns a file object denoting the XMLIndex home directory
   * 
   * @throws FileNotFoundException
   */
  private void initHomeDir() {    
    String home = null;
    // Try JNDI
    try {
      javax.naming.Context c = new InitialContext();
      home = (String) c.lookup("java:comp/env/" + WebDefinitions.PROJECT_NAME + "/home");
      logger.info("Using JNDI " + WebDefinitions.PROJECT_NAME + ".home: " + home);
    } catch (NoInitialContextException e) {
      logger.info("JNDI not configured for " + WebDefinitions.PROJECT_NAME + " (NoInitialContextEx)");
    } catch (NamingException e) {
      logger.info("No /" + WebDefinitions.PROJECT_NAME + "/home in JNDI");
    } catch( RuntimeException ex ) {
      logger.warn("Odd RuntimeException while testing for JNDI: " + ex.getMessage());
    } 
    
    // Now try system property
    if (home == null) {
      String prop = WebDefinitions.PROJECT_NAME + ".home";
      home = System.getProperty(prop);
      if (home != null) {
        logger.info("Using system property " + prop + ": " + home);
      }
    }
     
    if (home == null) {
      String error = "FATAL: Could not find system property or JNDI for \"" + WebDefinitions.PROJECT_NAME + ".home\"";
      logger.error(error);
      throw new XMLIndexException(error);
    }
    homeDir = new File(home);
    if (!homeDir.isDirectory()) {
      String error = "FATAL: Directory \"" + WebDefinitions.PROJECT_NAME + ".home\" not found";
      logger.error(error);
      throw new XMLIndexException(error);
    }    
  }
  
  private void initProperties() throws Exception {
    File propsFile = new File(homeDir, WebDefinitions.FILENAME_PROPERTIES);
    this.properties = XMLIndexWebUtils.readProperties(propsFile);
    this.trustAllCerts = new Boolean(properties.getProperty(WebDefinitions.PROPERTYNAME_TRUST_ALL_CERTS, "false"));
    if (trustAllCerts) {
      TrustManager[] trustAllCertsManager = new TrustManager[] { new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
      }};
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCertsManager, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
    this.developmentMode = new Boolean(this.properties.getProperty(WebDefinitions.PROPERTYNAME_DEVELOPMENTMODE, "false"));
    this.parserHardening = new Boolean(this.properties.getProperty(WebDefinitions.PROPERTYNAME_PARSER_HARDENING, "false"));
    String indexesPath = this.properties.getProperty(WebDefinitions.PROPERTYNAME_INDEXPATH, "indexes");
    this.indexesDir = new File(indexesPath);
    if (!this.indexesDir.isAbsolute())
      this.indexesDir = new File(this.homeDir, indexesPath);
    if (!this.indexesDir.exists())
      if (!this.indexesDir.mkdirs()) 
        throw new XMLIndexException("Could not find or create directory for indexes \"" + indexesDir + "\"");
  }
   
  /*
  private void initFileAlterationObservers() {
    File webAppsDir = new File(homeDir, "webapps");        
    IOFileFilter webAppFiles = FileFilterUtils.and(FileFilterUtils.fileFileFilter(), FileFilterUtils.nameFileFilter("webapp.xml"));    
    IOFileFilter filter = FileFilterUtils.or(FileFilterUtils.directoryFileFilter(), webAppFiles);    
    FileAlterationObserver webAppObserver = new FileAlterationObserver(webAppsDir, filter);
    webAppObserver.addListener(new FileAlterationListenerAdaptor() {

      @Override
      public void onFileCreate(File file) {
        logger.info("New webapp detected ..."); 
        reloadWebApp(file, true);
      }

      @Override
      public void onFileChange(File file) {
        logger.info("Change in webapp definition detected ...");
        reloadWebApp(file, true);
      }

      @Override
      public void onFileDelete(File file) {
        logger.info("Deletion of webapp detected ...");
        reloadWebApp(file, true);
      }
      
    });
    
    monitor = new FileAlterationMonitor(3000);
    monitor.addObserver(webAppObserver);    
  }
  */
  
  public ServletContext getServletContext() {
    return this.servletContext;
  }
  
  public File getHomeDir() {
    return this.homeDir;
  }
  
  public Properties getProperties() {     
    return this.properties;    
  }
  
  public boolean getDevelopmentMode() {
    return this.developmentMode;
  }
  
  public boolean getParserHardening() {     
    return this.parserHardening;    
  }
  
  public boolean getTrustAllCerts() {     
    return this.trustAllCerts;    
  }
  
  public File getIndexesDir() {
    return indexesDir;
  }
  
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
    this.contextPath = servletContext.getContextPath();
    this.webInfDir = new File(servletContext.getRealPath("/WEB-INF"));
  }
  
  public String getContextPath() {
    return this.contextPath;
  }
  
  public File getWebInfDir() {
    return this.webInfDir;
  }
  
  public boolean isOpen() {
    return isOpen;
  }
  
  public synchronized XMLIndex getIndex(String name) throws IOException {
    XMLIndex index = indexes.get(name);
    if (index == null) {
      File indexDir = new File(getIndexesDir(), name);
      if (!indexDir.isDirectory())
        throw new FileNotFoundException("Index directory \"" + indexDir.getAbsolutePath() + "\" not found");
      index = new XMLIndex(name, indexDir.toPath());
      indexes.put(name, index);
    }
    return index;
  }
  
}