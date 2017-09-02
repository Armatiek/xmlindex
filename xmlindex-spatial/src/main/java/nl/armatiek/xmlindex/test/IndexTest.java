package nl.armatiek.xmlindex.test;

import java.nio.file.Paths;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;

public class IndexTest {
  
  private static final Logger logger = LoggerFactory.getLogger(IndexTest.class);
  
  private void runTest(String indexPath, String gmlFolder) throws Exception {
    XMLIndex index = new XMLIndex("Test", Paths.get(indexPath));
    index.open();
    try {
      Session session = index.aquireSession();
      try {
        logger.info("Starting indexing GML file ...");
        StopWatch sw = new StopWatch();
        sw.start();
        session.addDocuments(Paths.get(gmlFolder), Integer.MAX_VALUE, "^(.*)\\.(gml)", null);
        session.commit();
        logger.info("Indexing execution time: " + sw.toString());
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
      test.runTest(args[0], args[1]);
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

}
