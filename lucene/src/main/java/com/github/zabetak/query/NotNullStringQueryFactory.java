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
package com.github.zabetak.query;

import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

public enum NotNullStringQueryFactory {
    Q0 {
        @Override
        public Query create(String fieldName) {
            return new WildcardQuery(new Term(fieldName, "*"));
        }
    },
    Q1 {
        @Override
        public Query create(String fieldName) {
            // After Query#rewrite this is exactly the same with Q2 below.
            return SortedSetDocValuesField.newSlowRangeQuery(fieldName, null, null, true, true);
        }
    },
    Q2 {
        @Override
        public Query create(String fieldName) {
            return new DocValuesFieldExistsQuery(fieldName);
        }
    };

    public abstract Query create(String fieldName);
}
