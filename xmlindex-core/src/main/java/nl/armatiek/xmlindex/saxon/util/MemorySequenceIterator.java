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

package nl.armatiek.xmlindex.saxon.util;

import java.util.ArrayList;
import java.util.Iterator;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.error.XMLIndexException;

public class MemorySequenceIterator implements SequenceIterator {

  private ArrayList<Item> items = new ArrayList<Item>();
  private Iterator<Item> itemsIter;
  
  public MemorySequenceIterator(SequenceIterator iter) {
    if (iter == null) {
      itemsIter = items.iterator();
      return;
    }
    try {
      Item item;
      while ((item = iter.next()) != null) 
        items.add(item);
      itemsIter = items.iterator();
    } catch (XPathException xpe) {
      throw new XMLIndexException("Error constructing MemorySequenceIterator", xpe);
    }
  }
  
  @Override
  public Item next() throws XPathException {
    return itemsIter.next();
  }

  @Override
  public void close() { }

  @Override
  public int getProperties() {
    return 0;
  }
  
  public boolean isEmpty() {
    return items.isEmpty();
  }

}