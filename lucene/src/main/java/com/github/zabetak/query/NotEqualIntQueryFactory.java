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

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.search.*;

public enum NotEqualIntQueryFactory {
    Q0 {
        @Override
        public Query create(String fieldName, int value) {
            Query all = new MatchAllDocsQuery();
            Query notNull = IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE, Integer.MAX_VALUE);
            Query isNull = minus(all, notNull);
            Query eq = IntPoint.newExactQuery(fieldName, value);
            return minus(all, or(eq, isNull));
        }
    },
    Q1 {
        @Override
        public Query create(String fieldName, int value) {
            Query eq = IntPoint.newExactQuery(fieldName, value);
            Query notNull = IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE, Integer.MAX_VALUE);
            return minus(notNull, eq);
        }
    },
    Q2 {
        @Override
        public Query create(String fieldName, int value) {
            if (value == Integer.MIN_VALUE)
                return IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
            if (value == Integer.MAX_VALUE)
                return IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE, Integer.MAX_VALUE - 1);
            Query lt = IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE, value - 1);
            Query gt = IntPoint.newRangeQuery(fieldName, value + 1, Integer.MAX_VALUE);
            return or(lt, gt);
        }
    },
    Q3 {
        @Override
        public Query create(String fieldName, int value) {
            if (value == Integer.MIN_VALUE)
                return NumericDocValuesField.newSlowRangeQuery(fieldName,
                        Integer.MIN_VALUE + 1,
                        Integer.MAX_VALUE);
            if (value == Integer.MAX_VALUE)
                return NumericDocValuesField.newSlowRangeQuery(fieldName,
                        Integer.MIN_VALUE,
                        Integer.MAX_VALUE - 1);
            Query lt = NumericDocValuesField.newSlowRangeQuery(fieldName, Integer.MIN_VALUE, value - 1);
            Query gt = NumericDocValuesField.newSlowRangeQuery(fieldName, value + 1, Integer.MAX_VALUE);
            return or(lt, gt);
        }
    },
    Q4 {
        @Override
        public Query create(String fieldName, int value) {
            Query notNull = new DocValuesFieldExistsQuery(fieldName);
            Query eq = IntPoint.newExactQuery(fieldName, value);
            return minus(notNull, eq);
        }
    },
    Q5 {
        @Override
        public Query create(String fieldName, int value) {
            Query notNull = new DocValuesFieldExistsQuery(fieldName);
            Query eq = NumericDocValuesField.newSlowExactQuery(fieldName, value);
            return minus(notNull, eq);
        }
    };

    public abstract Query create(String fieldName, int value);

    private static Query minus(Query must, Query mustNot) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(must, BooleanClause.Occur.MUST);
        builder.add(mustNot, BooleanClause.Occur.MUST_NOT);
        return builder.build();
    }

    private static Query or(Query... queries) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (Query q : queries)
            builder.add(q, BooleanClause.Occur.SHOULD);
        return builder.build();
    }
}
