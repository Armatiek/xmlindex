package nl.armatiek.xmlindex.tests.core;

import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.plugins.convertor.FileConvertor;

public class IndexTest {
  
  private static final Logger logger = LoggerFactory.getLogger(IndexTest.class);
  
  private void runTest(String indexPath, String importPath, String pattern) throws Exception {
    XMLIndex index = new XMLIndex("Test", Paths.get(indexPath));
    index.open();
    try {
      Session session = index.aquireSession();
      try {
        logger.info("Starting batch indexing ...");
        StopWatch sw = new StopWatch();
        sw.start();
        HashMap<String, FileConvertor> fileSpecs = new HashMap<String, FileConvertor>();
        fileSpecs.put("(.*)\\.(xml|WTI)$", null);
        session.addDocuments(Paths.get(importPath), Integer.MAX_VALUE, fileSpecs, true, null);
        session.commit();
        logger.info("Batch indexing execution time: " + sw.toString());
        sw.stop();
      } finally {
        index.returnSession(session);
      }
    } finally {
      index.close();
    }
  }

  public static void main(String[] args) {
    try {
      IndexTest test = new IndexTest();
      test.runTest(args[0], args[1], args[2]);
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

}
