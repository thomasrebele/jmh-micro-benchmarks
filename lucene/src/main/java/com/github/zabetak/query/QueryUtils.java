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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

abstract class QueryUtils {
    private QueryUtils() {

    }

    static Query minus(Query must, Query mustNot) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(must, BooleanClause.Occur.MUST);
        builder.add(mustNot, BooleanClause.Occur.MUST_NOT);
        return builder.build();
    }

    static Query or(Query... queries) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (Query q : queries)
            builder.add(q, BooleanClause.Occur.SHOULD);
        return builder.build();
    }
}
