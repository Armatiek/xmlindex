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
import java.util.ListIterator;

import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.valuesource.LongFieldSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import nl.armatiek.xmlindex.conf.Definitions;

public class HitQuery {
  
  private final IndexSearcher searcher;
  private final Query query;
  private final boolean reversed;
  
  public HitQuery(IndexSearcher searcher, Query query, boolean reversed) {
    this.searcher= searcher;
    this.query = query;
    this.reversed = reversed;
  }
  
  public HitQuery(IndexSearcher searcher, Query query) {
    this(searcher, query, false);
  }
  
  public HitIterator execute() throws IOException {
    DocumentOrderCollector collector = new DocumentOrderCollector(reversed);
    Query documentOrderQuery = new CustomScoreQuery(query, new FunctionQuery(new LongFieldSource(Definitions.FIELDNAME_LEFT)));
    searcher.search(documentOrderQuery, collector);
    ListIterator<Hit> hits = collector.getHits();
    return new HitIterator(hits, collector.getNumberOfNodes(), reversed);
  }

}
