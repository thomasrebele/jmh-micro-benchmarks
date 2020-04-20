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
package com.github.zabetak.benchmark;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;

import java.io.IOException;

/**
 * The mode that is used to return results from an {@link org.apache.lucene.search.IndexSearcher}.
 */
public enum SearchMode {
    ALL,
    TOP_K,
    SORT_K;

    public void execute(IndexSearcher searcher, Query q, Sort sort, int k) throws IOException {
        switch (this) {
            case ALL:
                searcher.search(q, ALL_MATCHES_COLLECTOR);
                break;
            case TOP_K:
                searcher.search(q, k);
                break;
            case SORT_K:
                searcher.search(q, k, sort);
                break;
            default:
                throw new AssertionError();
        }
    }

    private static final LeafCollector DO_NOTHING_LEAF_COLLECTOR = new LeafCollector() {
        @Override
        public void setScorer(Scorable scorer) throws IOException {

        }

        @Override
        public void collect(int doc) throws IOException {

        }
    };

    private static final Collector ALL_MATCHES_COLLECTOR = new Collector() {
        @Override
        public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
            return DO_NOTHING_LEAF_COLLECTOR;
        }

        @Override
        public ScoreMode scoreMode() {
            return ScoreMode.COMPLETE_NO_SCORES;
        }
    };
}
