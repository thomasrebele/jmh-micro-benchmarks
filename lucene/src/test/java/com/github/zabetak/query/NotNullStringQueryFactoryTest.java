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
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NotNullStringQueryFactoryTest {
    private static final String FIELD_NAME = "firstName";

    @Test
    public void testCorrectResults() throws IOException {
        Path indexPath = Paths.get(System.getProperty("java.io.tmpdir"), "jmh-lucene-indexes",
                NotNullStringQueryFactoryTest.class.getSimpleName());
        try (Directory dir = FSDirectory.open(indexPath)) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                writer.addDocument(new Document());
                writer.addDocument(newDocument(""));
                writer.addDocument(newDocument("Victor"));
                writer.addDocument(newDocument("Victor"));
                writer.addDocument(newDocument("Alex"));
                writer.commit();
            }
            try (IndexReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                for (NotNullStringQueryFactory factory : NotNullStringQueryFactory.values()) {
                    Query q = factory.create(FIELD_NAME);
                    TopDocs matches = searcher.search(q, 5);
                    assertThat(matches.scoreDocs.length, is(4));
                    assertThat(matches.scoreDocs[0].doc, is(1));
                    assertThat(matches.scoreDocs[1].doc, is(2));
                    assertThat(matches.scoreDocs[2].doc, is(3));
                    assertThat(matches.scoreDocs[3].doc, is(4));
                }
            }
        }
    }

    @Test
    public void testQ0CorrectQuery() {
        Query q = NotNullStringQueryFactory.Q0.create(FIELD_NAME);
        assertThat(q.toString(), is("firstName:*"));
    }

    @Test
    public void testQ1CorrectQuery() {
        Query q = NotNullStringQueryFactory.Q1.create(FIELD_NAME);
        assertThat(q.toString(), is("firstName:{* TO *}"));
    }

    @Test
    public void testQ2CorrectQuery() {
        Query q = NotNullStringQueryFactory.Q2.create(FIELD_NAME);
        assertThat(q.toString(), is("DocValuesFieldExistsQuery [field=firstName]"));
    }

    private Document newDocument(String fieldValue) {
        Document doc = new Document();
        StringField fieldA = new StringField(FIELD_NAME, fieldValue, Field.Store.YES);
        SortedDocValuesField fieldB = new SortedDocValuesField(FIELD_NAME, new BytesRef(fieldValue));
        doc.add(fieldA);
        doc.add(fieldB);
        return doc;
    }
}
