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

package nl.armatiek.xmlindex.query;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.search.BooleanClause;

public class BooleanQueryDef extends QueryDef {
  
  private ArrayList<BooleanClauseDef> clauses = new ArrayList<BooleanClauseDef>();
  
  public void add(QueryDef queryDef, BooleanClause.Occur occur) {
    clauses.add(new BooleanClauseDef(queryDef, occur));
  }
  
  public void add(BooleanClauseDef clause) {
    clauses.add(clause);
  }

  public Iterator<BooleanClauseDef> getClauses() {
    return clauses.iterator();
  }
  
}