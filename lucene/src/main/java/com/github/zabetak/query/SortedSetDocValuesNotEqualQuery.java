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

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Objects;

/**
 * A query matching all documents not equal to a given value.
 */
public final class SortedSetDocValuesNotEqualQuery extends Query {

    private final String field;
    private final BytesRef value;

    public SortedSetDocValuesNotEqualQuery(String field, BytesRef value) {
        this.field = Objects.requireNonNull(field);
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (!sameClassAs(obj)) {
            return false;
        }
        SortedSetDocValuesNotEqualQuery that = (SortedSetDocValuesNotEqualQuery) obj;
        return Objects.equals(field, that.field) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int h = classHash();
        h = 31 * h + field.hashCode();
        h = 31 * h + Objects.hashCode(value);
        return h;
    }

    @Override
    public String toString(String field) {
        StringBuilder b = new StringBuilder();
        if (!this.field.equals(field))
            b.append(this.field).append(":");
        b.append("!=");
        b.append(value);
        return b.toString();
    }

    SortedSetDocValues getValues(LeafReader reader, String field) throws IOException {
        return DocValues.singleton(DocValues.getSorted(reader, field));
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) {
        return new ConstantScoreWeight(this, boost) {
            @Override
            public Scorer scorer(LeafReaderContext context) throws IOException {
                SortedSetDocValues values = getValues(context.reader(), field);

                long exclude = values.lookupTerm(value);
                if (exclude == -1)
                    return new ConstantScoreScorer(this, score(), scoreMode, values);


                final SortedDocValues singleton = DocValues.unwrapSingleton(values);
                final TwoPhaseIterator iterator;
                if (singleton != null) {
                    iterator = new TwoPhaseIterator(singleton) {
                        @Override
                        public boolean matches() throws IOException {
                            final long ord = singleton.ordValue();
                            return ord != exclude;
                        }

                        @Override
                        public float matchCost() {
                            return 1; // 1 comparison
                        }
                    };
                } else {
                    iterator = new TwoPhaseIterator(values) {
                        @Override
                        public boolean matches() throws IOException {
                            long ord = values.nextOrd();
                            if (ord != SortedSetDocValues.NO_MORE_ORDS)
                                return ord != exclude;
                            return false;
                        }

                        @Override
                        public float matchCost() {
                            return 1; // 1 comparison
                        }
                    };
                }
                return new ConstantScoreScorer(this, score(), scoreMode, iterator);
            }

            @Override
            public boolean isCacheable(LeafReaderContext ctx) {
                return DocValues.isCacheable(ctx, field);
            }
        };
    }

}
