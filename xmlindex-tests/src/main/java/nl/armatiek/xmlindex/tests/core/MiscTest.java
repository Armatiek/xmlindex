package nl.armatiek.xmlindex.tests.core;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.om.Item;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.tree.iter.UnfailingIterator;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.node.DocumentElement;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class MiscTest {
  
  private static final Logger logger = LoggerFactory.getLogger(MiscTest.class);
  
  private void runTest(String indexPath) throws IOException, SaxonApiException {
    XMLIndex index = new XMLIndex("Test", Paths.get(indexPath));
    index.open();
    try {
      Session session = index.aquireSession();
      try {
        logger.info("Start of test ...");
        StopWatch sw = new StopWatch();
        sw.start();
        
        UnfailingIterator iter = session.getAllDocuments();
        Item item;
        while ((item = iter.next()) != null) {
          System.out.println(((DocumentElement)((XMLIndexNodeInfo) item).getRealNode()).uri);
        }
        
        logger.info("Test execution time: " + sw.toString());
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
      MiscTest test = new MiscTest();
      test.runTest(args[0]);
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

}
