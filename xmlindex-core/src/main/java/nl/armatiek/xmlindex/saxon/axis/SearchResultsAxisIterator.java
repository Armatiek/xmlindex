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

package nl.armatiek.xmlindex.saxon.axis;

import java.io.IOException;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.LookaheadIterator;
import nl.armatiek.xmlindex.Session;
import nl.armatiek.xmlindex.error.XMLIndexException;
import nl.armatiek.xmlindex.lucene.query.HitIterator;
import nl.armatiek.xmlindex.lucene.query.HitQuery;
import nl.armatiek.xmlindex.node.Node;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public abstract class SearchResultsAxisIterator implements AxisIterator, LastPositionFinder<XMLIndexNodeInfo>, LookaheadIterator {
  
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(SearchResultsAxisIterator.class);
  
  protected Session session;
  protected IndexSearcher searcher;
  protected XMLIndexNodeInfo contextNode;
  protected Node node;
  protected NodeTest nodeTest;
  protected boolean includeSelf;
  protected boolean reversed;
  protected XPathContext xpathContext;
  protected HitIterator hitIterator;
  
  public SearchResultsAxisIterator(Session session, XMLIndexNodeInfo contextNode, 
      NodeTest nodeTest, boolean includeSelf, boolean reversed, XPathContext xpathContext) {
    try {
      this.session = session;
      this.searcher = session.getIndexSearcher();
      this.contextNode = contextNode;
      this.node = (Node) contextNode.getRealNode();
      this.nodeTest = nodeTest;
      this.includeSelf = includeSelf;
      this.reversed = reversed;
      this.xpathContext = xpathContext;
      this.hitIterator = getHits();
    } catch (IOException | XPathException e) {
      throw new XMLIndexException(e.getMessage(), e);
    }
  }
    
  protected abstract void addAxisClauses(Builder queryBuilder);
  
  protected abstract XMLIndexNodeInfo getXMLIndexNodeInfo(Node node);
  
  protected HitIterator getHits() throws IOException, XPathException {
    Builder queryBuilder = new BooleanQuery.Builder();
    addAxisClauses(queryBuilder);
    QueryConstructor.addNodeTestClauses(queryBuilder, nodeTest, Occur.FILTER, session, xpathContext);
    Query query = queryBuilder.build();
    HitQuery hitQuery = new HitQuery(searcher, query, reversed);
    return hitQuery.execute();
  }
  
  /* AxisIterator */
  @Override
  public XMLIndexNodeInfo next() {
    if (hitIterator == null || !hitIterator.hasNext())
      return null;
    try {
      Document doc = searcher.doc(hitIterator.next().docId);
      Node node = Node.createNode(doc, session);
      return getXMLIndexNodeInfo(node);
    } catch (IOException ioe) {
      throw new XMLIndexException(ioe);
    }
  }

  @Override
  public void close() { }

  @Override
  public int getProperties() {
    return SequenceIterator.LOOKAHEAD | SequenceIterator.LAST_POSITION_FINDER;    
  }

  /* LastPositionFinder */
  @Override
  public int getLength() throws XPathException {
    return hitIterator != null ? hitIterator.size() : 0;
  }
  
  /* LookaheadIterator */
  @Override
  public boolean hasNext() {
    return hitIterator == null ? false : hitIterator.hasNext();        
  }
  
}