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
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;

import static com.github.zabetak.query.QueryUtils.minus;
import static com.github.zabetak.query.QueryUtils.or;

/**
 * A factory creating Lucene queries for performing not equal comparisons on string fields.
 * <p>
 * The queries follow the SQL semantics, that is {@code field <> 'Victor'} returns all documents
 * with values that are not equal to 'Victor' but not those documents that do not have a value for
 * this field; in other words excluding {@code null} values.
 * </p>
 */
public enum NotEqualStringQueryFactory {
    Q0 {
        @Override
        public Query create(String fieldName, String value) {
            Query all = new MatchAllDocsQuery();
            Query isNull = minus(all, new WildcardQuery(new Term(fieldName, "*")));
            return minus(all, or(new TermQuery(new Term(fieldName, value)), isNull));
        }
    },
    Q1 {
        @Override
        public Query create(String fieldName, String value) {
            Query eq = new TermQuery(new Term(fieldName, value));
            Query notNull = new WildcardQuery(new Term(fieldName, "*"));
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(notNull, BooleanClause.Occur.MUST);
            builder.add(eq, BooleanClause.Occur.MUST_NOT);
            return builder.build();
        }
    },
    Q2 {
        @Override
        public Query create(String fieldName, String value) {
            Query eq = new TermQuery(new Term(fieldName, value));
            Query notNull = new DocValuesFieldExistsQuery(fieldName);
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(notNull, BooleanClause.Occur.MUST);
            builder.add(eq, BooleanClause.Occur.MUST_NOT);
            return builder.build();
        }
    },
    Q3 {
        @Override
        public Query create(String fieldName, String value) {
            return new InverseTermQuery(new Term(fieldName, value));
        }
    },
    Q4 {
        @Override
        public Query create(String fieldName, String value) {
            return new SortedSetDocValuesNotEqualQuery(fieldName, new BytesRef(value));
        }
    },
    Q5 {
        @Override
        public Query create(String fieldName, String value) {
            Query lt = SortedSetDocValuesField.newSlowRangeQuery(fieldName, null,
                    new BytesRef(value), true, false);
            Query gt = SortedSetDocValuesField.newSlowRangeQuery(fieldName, new BytesRef(value),
                    null, false, true);
            return or(lt, gt);
        }
    };

    public abstract Query create(String fieldName, String value);

}
