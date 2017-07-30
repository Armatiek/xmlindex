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

package nl.armatiek.xmlindex.lucene.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;

public class DocumentOrderCollector implements Collector {
  
  private final ArrayList<Hit> hits = new ArrayList<Hit>();
  
  private final boolean reversed;
  
  public DocumentOrderCollector(boolean reversed) {
    this.reversed = reversed;
  }
  
  @Override
  public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
    
    final int docBase = context.docBase;
    
    return new LeafCollector() {
      
      private Scorer scorer;
      
      @Override
      public void setScorer(Scorer scorer) throws IOException {
        this.scorer = scorer;
      }

      @Override
      public void collect(int docId) throws IOException {
        hits.add(new Hit(docBase + docId, scorer.score()));
      }
      
    };
  }

  @Override
  public boolean needsScores() {
    return true;
  }

  public int getNumberOfNodes() {
    return hits.size();
  }
  
  public ListIterator<Hit> getHits() {
    Collections.sort(hits, new Comparator<Hit>() {
      
      @Override
      public int compare(Hit h1, Hit h2) {
        if (h1.score < h2.score)
          return -1;
        if (h1.score > h2.score)
          return 1;
        return 0;
      }

    });
    
    if (reversed)
      return hits.listIterator(hits.size());
    return hits.listIterator();
  }
  
}
