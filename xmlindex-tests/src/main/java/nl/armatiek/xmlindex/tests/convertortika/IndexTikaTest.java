package nl.armatiek.xmlindex.tests.convertortika;

import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.extensions.TikaConvertor;
import nl.armatiek.xmlindex.plugins.convertor.FileConvertor;

public class IndexTikaTest {
  
  private static final Logger logger = LoggerFactory.getLogger(IndexTikaTest.class);
  
  private void runTest(String indexPath, String importPath) throws Exception {
    XMLIndex index = new XMLIndex("IndexTikaTest", Paths.get(indexPath));
    index.open();
    try {
      Session session = index.aquireSession();
      try {
        logger.info("Starting batch indexing ...");
        StopWatch sw = new StopWatch();
        sw.start();
        HashMap<String, FileConvertor> fileSpecs = new HashMap<String, FileConvertor>();
        fileSpecs.put("(.*)\\.xml$", null);
        fileSpecs.put("(.*)\\.pdf$", new TikaConvertor("tika"));
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
      IndexTikaTest test = new IndexTikaTest();
      test.runTest(args[0], args[1]);
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

}
