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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IsNullIntQueryFactoryTest {
    private static final String FIELD_NAME = "age";

    @Test
    public void testCorrectResults() throws IOException {
        Path indexPath = Paths.get(System.getProperty("java.io.tmpdir"), "jmh-lucene-indexes",
                IsNullIntQueryFactoryTest.class.getSimpleName());
        try (Directory dir = FSDirectory.open(indexPath)) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                writer.addDocument(new Document());
                writer.addDocument(newDocument(18));
                writer.addDocument(newDocument(28));
                writer.addDocument(newDocument(28));
                writer.addDocument(newDocument(Integer.MAX_VALUE));
                writer.addDocument(newDocument(Integer.MIN_VALUE));
                writer.commit();
            }
            try (IndexReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                for (IsNullIntQueryFactory factory : IsNullIntQueryFactory.values()) {
                    Query q = factory.create(FIELD_NAME);
                    TopDocs matches = searcher.search(q, 5);
                    assertThat(matches.scoreDocs.length, is(1));
                    assertThat(matches.scoreDocs[0].doc, is(0));
                }
            }
        }
    }

    @Test
    public void testQ0CorrectQuery() {
        Query q = IsNullIntQueryFactory.Q0.create(FIELD_NAME);
        assertThat(q.toString(), is("+*:* -age:[-2147483648 TO 2147483647]"));
    }

    @Test
    public void testQ1CorrectQuery() {
        Query q = IsNullIntQueryFactory.Q1.create(FIELD_NAME);
        assertThat(q.toString(), is("+*:* -age:[-2147483648 TO 2147483647]"));
    }

    @Test
    public void testQ2CorrectQuery() {
        Query q = IsNullIntQueryFactory.Q2.create(FIELD_NAME);
        assertThat(q.toString(), is("+*:* -DocValuesFieldExistsQuery [field=age]"));
    }

    private Document newDocument(int fieldValue) {
        Document doc = new Document();
        IntPoint fieldA = new IntPoint(FIELD_NAME, fieldValue);
        NumericDocValuesField fieldB = new NumericDocValuesField(FIELD_NAME, fieldValue);
        StoredField fieldC = new StoredField(FIELD_NAME, fieldValue);
        doc.add(fieldA);
        doc.add(fieldB);
        doc.add(fieldC);
        return doc;
    }
}
