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

import com.github.zabetak.indexer.IndexGenerator;
import com.github.zabetak.indexer.IndexPathBuilder;
import com.github.zabetak.indexer.RandomStringValueFieldFactory;
import com.github.zabetak.query.NotEqualStringQueryFactory;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * A benchmark of various ways to evaluate the SQL not equal operator ({@code <>}) on string values
 * residing in Lucene indexes.
 * <p>
 * The not equal comparison strictly follows the SQL semantics, that is {@code field <> 'Victor'}
 * returns all documents with values that are not equal to 'Victor' but not those documents that do
 * not have a value for this field; in other words excluding {@code null} values.
 * </p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0, time = 1)
@Measurement(iterations = 5, time = 1)
@Timeout(time = 1, timeUnit = TimeUnit.MINUTES)
@Fork(1)
public class NotEqualOperatorOnStringBenchmark {

    private static final int PK_STRING_LENGTH = 20;
    private static final int TOP_K = 10;

    public enum QueryField {
        PK_STRING("pk_str_field"),
        BOOL_STRING("bool_str_field");

        private final String fieldName;

        QueryField(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    public enum ResultMode {
        ALL,
        TOPK
    }

    @State(Scope.Benchmark)
    public static class IndexState {
        @Param({"1000000", "10000000", "100000000"})
        public int docNumber;
        @Param({"10"})
        public int nullPercent;
        @Param
        public ResultMode mode;

        Path indexPath;

        @Setup(Level.Trial)
        public void setupIndex() throws IOException {
            IndexPathBuilder pathBuilder =
                    new IndexPathBuilder(NotEqualOperatorOnStringBenchmark.class);
            pathBuilder.setDocNumber(docNumber);
            pathBuilder.setStringLength(PK_STRING_LENGTH);
            pathBuilder.setNullPercent(nullPercent);
            indexPath = pathBuilder.build();
            IndexGenerator indexGenerator = new IndexGenerator();
            indexGenerator.setOverride(false);
            indexGenerator.setIndexPath(indexPath);
            indexGenerator.setDocNumber(docNumber);
            indexGenerator.setNullPercent(nullPercent);
            StringField uStringField =
                    new StringField(QueryField.PK_STRING.fieldName, "", Field.Store.YES);
            indexGenerator.addFieldFactory(RandomStringValueFieldFactory.of(uStringField, PK_STRING_LENGTH));
            SortedDocValuesField uniqDocValuesField =
                    new SortedDocValuesField(QueryField.PK_STRING.fieldName, new BytesRef());
            indexGenerator.addFieldFactory(RandomStringValueFieldFactory.of(uniqDocValuesField, PK_STRING_LENGTH));
            StringField boolStringField =
                    new StringField(QueryField.BOOL_STRING.fieldName, "", Field.Store.YES);
            indexGenerator.addFieldFactory(valueContext -> {
                if (valueContext.docId() % 2 == 0)
                    boolStringField.setStringValue("TRUE");
                else
                    boolStringField.setStringValue("FALSE");
                return boolStringField;
            });
            SortedDocValuesField boolDocValuesField =
                    new SortedDocValuesField(QueryField.BOOL_STRING.fieldName, new BytesRef());
            indexGenerator.addFieldFactory(valueContext -> {
                if (valueContext.docId() % 2 == 0)
                    boolDocValuesField.setBytesValue("TRUE".getBytes());
                else
                    boolDocValuesField.setBytesValue("FALSE".getBytes());
                return boolDocValuesField;
            });
            indexGenerator.createIndex();
        }

        private Directory readerDir;
        private IndexReader reader;
        private IndexSearcher searcher;
        private String uniqFieldValue;

        @Setup(Level.Iteration)
        public void setupSearcher() throws IOException {
            readerDir = FSDirectory.open(indexPath);
            reader = DirectoryReader.open(readerDir);
            searcher = new IndexSearcher(reader);
            TopDocs docs =
                    searcher.search(new WildcardQuery(new Term(QueryField.PK_STRING.fieldName, "*")), 1);
            uniqFieldValue =
                    searcher.doc(docs.scoreDocs[0].doc).getField(QueryField.PK_STRING.fieldName).stringValue();
        }

        @TearDown(Level.Iteration)
        public void cleanupSearcher() throws IOException {
            if (reader != null)
                reader.close();
            if (readerDir != null)
                readerDir.close();
            readerDir = null;
            reader = null;
            searcher = null;
            uniqFieldValue = null;
        }

        public String fieldValue(QueryField field) {
            switch (field) {
                case PK_STRING:
                    return uniqFieldValue;
                case BOOL_STRING:
                    return "TRUE";
            }
            throw new AssertionError();
        }

        public void execute(Query q) throws IOException {
            switch (mode) {
                case ALL:
                    searcher.count(q);
                    break;
                case TOPK:
                    searcher.search(q, TOP_K);
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }

    @State(Scope.Benchmark)
    public static class QueryState {
        @Param
        public NotEqualStringQueryFactory queryFactory;
        @Param
        public QueryField field;
    }


    @Benchmark
    public void execute(QueryState qState, IndexState iState) throws IOException {
        QueryField field = qState.field;
        Query q = qState.queryFactory.create(field.fieldName, iState.fieldValue(field));
        iState.execute(q);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(NotEqualOperatorOnStringBenchmark.class.getSimpleName())
                .forks(1).build();
        new Runner(opt).run();
    }
}
