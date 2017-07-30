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

import java.util.Iterator;
import java.util.ListIterator;

public class HitIterator implements Iterator<Hit> {
  
  private final ListIterator<Hit> hits;
  private final int numHits;
  private final boolean reversed;
  
  public HitIterator(ListIterator<Hit> hits, int numHits, boolean reversed) {
    this.hits = hits;
    this.numHits = numHits;
    this.reversed = reversed;
  }

  @Override
  public boolean hasNext() {
    if (reversed)
      return hits.hasPrevious();
    return hits.hasNext();
  }

  @Override
  public Hit next() {
    if (reversed)
      return hits.previous();
    return hits.next();
  }
  
  public int size() {
    return numHits;
  }
  
}