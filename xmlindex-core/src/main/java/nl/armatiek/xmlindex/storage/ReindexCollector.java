package nl.armatiek.xmlindex.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;

import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.XMLIndex;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class ReindexCollector implements Collector {
  
  private final Session session;
  private final XMLIndex index;
  private final Map<Long, String> baseUriMap = new HashMap<Long, String>();
  private final Object indexObject;
  
  public ReindexCollector(Session session, XMLIndex index, Object indexObject) {
    this.session = session;
    this.index = index;
    this.indexObject = indexObject;
  }

  @Override
  public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
    
    final int docBase = context.docBase;
    
    return new LeafCollector() {
      @Override
      public void setScorer(Scorer scorer) throws IOException { }

      @Override
      public void collect(int docID) throws IOException {
        try {
          Document doc = session.getIndexSearcher().doc((int) docBase + docID);
          index.getNodeStore().reindexNode(session, doc, baseUriMap);
        } catch (Exception e) {
          throw new XMLIndexException("Error reindexing \"" + indexObject.toString() + "\"", e);
        }
      }
    };
  }

  @Override
  public boolean needsScores() {
    return false;
  }

}