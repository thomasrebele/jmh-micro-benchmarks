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
import org.apache.lucene.util.DocIdSetBuilder;

import java.io.IOException;

/**
 * A Query that matches documents not containing a term.
 */
public class InverseTermQuery extends Query {

    private final Term term;

    public InverseTermQuery(Term term) {
        this.term = term;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        return new ConstantScoreWeight(this, boost) {

            @Override
            public boolean isCacheable(LeafReaderContext ctx) {
                return true;
            }

            @Override
            public Scorer scorer(LeafReaderContext context) throws IOException {
                Terms terms = context.reader().terms(term.field());
                if (terms == null)
                    return null;
                DocIdSetBuilder builder = new DocIdSetBuilder(context.reader().maxDoc(), terms);
                TermsEnum termsEnum = terms.iterator();
                PostingsEnum docs = null;
                BytesRef t = termsEnum.next();
                while (t != null) {
                    if (!t.equals(term.bytes())) {
                        docs = termsEnum.postings(docs, PostingsEnum.NONE);
                        builder.add(docs);
                    }
                    t = termsEnum.next();
                }
                DocIdSet set = builder.build();
                return new ConstantScoreScorer(this, score(), scoreMode, set.iterator());
            }
        };
    }

    /**
     * Prints a user-readable version of this query.
     */
    @Override
    public String toString(String field) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("!");
        if (!term.field().equals(field)) {
            buffer.append(term.field());
            buffer.append(":");
        }
        buffer.append(term.text());
        return buffer.toString();
    }

    /**
     * Returns true iff <code>other</code> is equal to <code>this</code>.
     */
    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) && term.equals(((InverseTermQuery) other).term);
    }

    @Override
    public int hashCode() {
        return classHash() ^ term.hashCode();
    }

}
