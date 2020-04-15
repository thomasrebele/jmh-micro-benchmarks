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
package com.github.zabetak.indexer;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class IndexGeneratorTest {

    @Test
    public void testDocNumber() throws IOException {
        final int docNumber = 100;
        IndexPathBuilder builder = new IndexPathBuilder(IndexGeneratorTest.class);
        builder.setDocNumber(docNumber);
        IndexGenerator generator = new IndexGenerator();
        generator.setDocNumber(docNumber);
        generator.setOverride(true);
        generator.setIndexPath(builder.build());
        generator.createIndex();

        try (Directory dir = FSDirectory.open(builder.build())) {
            try (IndexReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                int total = searcher.count(new MatchAllDocsQuery());
                assertThat(total, is(100));
            }
        }
    }

    @Test
    public void testNullPercent() throws IOException {
        final int docNumber = 100;
        final int nullPercent = 10;

        IndexPathBuilder builder = new IndexPathBuilder(IndexGeneratorTest.class);
        builder.setDocNumber(docNumber);
        builder.setNullPercent(nullPercent);
        IndexGenerator generator = new IndexGenerator();
        generator.setDocNumber(docNumber);
        generator.setOverride(true);
        generator.setIndexPath(builder.build());
        generator.setNullPercent(nullPercent);
        StringField field = new StringField("strField", "A", Field.Store.YES);
        generator.addFieldFactory(valueContext -> field);
        generator.createIndex();

        try (Directory dir = FSDirectory.open(builder.build())) {
            try (IndexReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                int total = searcher.count(new MatchAllDocsQuery());
                assertThat(total, is(100));
                final TermQuery valueQuery = new TermQuery(new Term("strField", "A"));
                int notNullNumber = searcher.count(valueQuery);
                assertThat(notNullNumber, allOf(greaterThan(85), lessThan(95)));
            }
        }
    }

    @Test
    public void testStringFieldCorrectValues() throws IOException {
        final int docNumber = 3;
        final String fieldName = "fieldA";
        IndexPathBuilder builder = new IndexPathBuilder(IndexGeneratorTest.class);
        builder.setDocNumber(docNumber);
        IndexGenerator generator = new IndexGenerator();
        generator.setDocNumber(docNumber);
        generator.setOverride(true);
        generator.setIndexPath(builder.build());
        StringField field = new StringField(fieldName, "A", Field.Store.YES);
        generator.addFieldFactory(valueContext -> {
            field.setStringValue("A" + valueContext.docId());
            return field;
        });
        generator.createIndex();

        try (Directory dir = FSDirectory.open(builder.build())) {
            try (IndexReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                assertThat(searcher.doc(0).get(fieldName), is("A0"));
                assertThat(searcher.doc(1).get(fieldName), is("A1"));
                assertThat(searcher.doc(2).get(fieldName), is("A2"));
            }
        }
    }

    @Test
    public void testSortedDocValuesFieldCorrectValues() throws IOException {
        final int docNumber = 3;
        final String fieldName = "fieldA";
        IndexPathBuilder builder = new IndexPathBuilder(IndexGeneratorTest.class);
        builder.setDocNumber(docNumber);
        IndexGenerator generator = new IndexGenerator();
        generator.setDocNumber(docNumber);
        generator.setOverride(true);
        generator.setIndexPath(builder.build());
        SortedDocValuesField field = new SortedDocValuesField(fieldName, new BytesRef());
        generator.addFieldFactory(valueContext -> {
            String value = "A" + valueContext.docId();
            field.setBytesValue(value.getBytes());
            return field;
        });
        generator.createIndex();

        try (Directory dir = FSDirectory.open(builder.build())) {
            try (IndexReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                String[] expectedValues = new String[]{"A0", "A1", "A2"};
                int[] expectedDocIds = new int[]{0, 1, 2};
                for (int i = 0; i < expectedValues.length; i++) {
                    String queryValue = expectedValues[i];
                    Query q = SortedDocValuesField.newSlowExactQuery(fieldName, new BytesRef(queryValue));
                    TopDocs docs = searcher.search(q, 1);
                    int docCnt = searcher.count(q);
                    assertThat(docCnt, is(1));
                    assertThat(docs.scoreDocs[0].doc, is(expectedDocIds[i]));
                }
            }
        }
    }
}
