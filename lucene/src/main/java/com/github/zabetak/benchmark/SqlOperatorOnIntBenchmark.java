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
import com.github.zabetak.indexer.RandomIntValueFieldFactory;
import com.github.zabetak.query.IsNullIntQueryFactory;
import com.github.zabetak.query.NotEqualIntQueryFactory;
import com.github.zabetak.query.NotNullIntQueryFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * A benchmark of various ways to evaluate the following SQL operators:
 * <ul>
 *     <li>NOT EQUAL ({@code <>}, {@code !=})</li>
 *     <li>IS NULL</li>
 *     <li>IS NOT NULL</li>
 * </ul>
 * on integer values residing in Lucene indexes.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0, time = 1)
@Measurement(iterations = 5, time = 1)
@Timeout(time = 1, timeUnit = TimeUnit.MINUTES)
@Fork(1)
public class SqlOperatorOnIntBenchmark {

    private static final int TOP_K = 10;
    private static final int AGE_LOWER_BOUND = 0;
    private static final int AGE_UPPER_BOUND = 110;

    public enum QueryField {
        PK_INT("pk_int_field"),
        AGE_INT("age_int_field");

        private final String fieldName;
        private final Sort sort;

        QueryField(String fieldName) {
            this.fieldName = fieldName;
            this.sort = new Sort(new SortField(fieldName, SortField.Type.INT));
        }
    }

    @State(Scope.Benchmark)
    public static class IndexState {
        @Param({"1000000", "10000000", "100000000"})
        public int docNumber;
        @Param({"10", "90"})
        public int nullPercent;

        Path indexPath;

        @Setup(Level.Trial)
        public void setupIndex() throws IOException {
            IndexPathBuilder pathBuilder =
                    new IndexPathBuilder(SqlOperatorOnIntBenchmark.class);
            pathBuilder.setDocNumber(docNumber);
            pathBuilder.setNullPercent(nullPercent);
            indexPath = pathBuilder.build();
            IndexGenerator indexGenerator = new IndexGenerator();
            indexGenerator.setOverride(false);
            indexGenerator.setIndexPath(indexPath);
            indexGenerator.setDocNumber(docNumber);
            indexGenerator.setNullPercent(nullPercent);
            IntPoint pkPointField = new IntPoint(QueryField.PK_INT.fieldName, 0);
            indexGenerator.addFieldFactory(valueContext -> {
                pkPointField.setIntValue(valueContext.docId());
                return pkPointField;
            });
            NumericDocValuesField pkDocField =
                    new NumericDocValuesField(QueryField.PK_INT.fieldName, 0L);
            indexGenerator.addFieldFactory(valueContext -> {
                pkDocField.setLongValue(valueContext.docId());
                return pkDocField;
            });
            StoredField pkStoreField = new StoredField(QueryField.PK_INT.fieldName, 0);
            indexGenerator.addFieldFactory(valueContext -> {
                pkStoreField.setIntValue(valueContext.docId());
                return pkStoreField;
            });
            IntPoint agePointField = new IntPoint(QueryField.AGE_INT.fieldName, 0);
            indexGenerator.addFieldFactory(RandomIntValueFieldFactory.withPoint(agePointField,
                    AGE_LOWER_BOUND, AGE_UPPER_BOUND));
            NumericDocValuesField ageDocField =
                    new NumericDocValuesField(QueryField.AGE_INT.fieldName, 0L);
            indexGenerator.addFieldFactory(RandomIntValueFieldFactory.withDocValues(ageDocField,
                    AGE_LOWER_BOUND, AGE_UPPER_BOUND));
            StoredField ageStoredField = new StoredField(QueryField.AGE_INT.fieldName, 0);
            indexGenerator.addFieldFactory(RandomIntValueFieldFactory.withStored(ageStoredField,
                    AGE_LOWER_BOUND, AGE_UPPER_BOUND));
            indexGenerator.createIndex();
        }

        private Directory readerDir;
        private IndexReader reader;
        private IndexSearcher searcher;
        private Integer pkValue;
        private Integer ageValue;

        @Setup(Level.Iteration)
        public void setupSearcher() throws IOException {
            readerDir = FSDirectory.open(indexPath);
            reader = DirectoryReader.open(readerDir);
            searcher = new IndexSearcher(reader);
            pkValue = searchFieldValue(QueryField.PK_INT);
            ageValue = searchFieldValue(QueryField.AGE_INT);
        }

        private Integer searchFieldValue(QueryField field) throws IOException {
            Query q = new DocValuesFieldExistsQuery(field.fieldName);
            TopDocs docs = searcher.search(q, 1);
            Document doc = searcher.doc(docs.scoreDocs[0].doc);
            return (Integer) doc.getField(field.fieldName).numericValue();
        }

        private Integer getValue(QueryField field) {
            switch (field) {
                case PK_INT:
                    return pkValue;
                case AGE_INT:
                    return ageValue;
            }
            throw new AssertionError();
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
            pkValue = null;
            ageValue = null;
        }
    }

    @State(Scope.Benchmark)
    public static class QueryTypeState {
        @Param
        public QueryField queryField;
        @Param
        public SearchMode searchMode;
    }

    @State(Scope.Benchmark)
    public static class NotEqualState {
        @Param
        public NotEqualIntQueryFactory queryFactory;
    }

    @State(Scope.Benchmark)
    public static class IsNotNullState {
        @Param
        public NotNullIntQueryFactory queryFactory;
    }

    @State(Scope.Benchmark)
    public static class IsNullState {
        @Param
        public IsNullIntQueryFactory queryFactory;
    }

    /**
     * Benchmark alternative ways to evaluate the NOT EQUAL operator.
     *
     * <p>
     * The not equal comparison strictly follows the SQL semantics, that is {@code field <> 5}
     * returns all documents with values that are not equal to 5 but not those documents that do not
     * have a value for this field; in other words excluding {@code null} values.
     * </p>
     */
    @Benchmark
    public void notEqual(NotEqualState notEqualState, QueryTypeState qState, IndexState iState) throws IOException {
        final QueryField field = qState.queryField;
        Query q = notEqualState.queryFactory.create(field.fieldName, iState.getValue(field));
        qState.searchMode.execute(iState.searcher, q, field.sort, TOP_K);
    }

    /**
     * Benchmark alternative ways to evaluate the IS NOT NULL operator.
     */
    @Benchmark
    public void isNotNull(IsNotNullState notNullState, QueryTypeState qState, IndexState iState) throws IOException {
        final QueryField field = qState.queryField;
        Query q = notNullState.queryFactory.create(field.fieldName);
        qState.searchMode.execute(iState.searcher, q, field.sort, TOP_K);
    }

    /**
     * Benchmark alternative ways to evaluate the IS NULL operator.
     */
    @Benchmark
    public void isNull(IsNullState isNullState, QueryTypeState qState, IndexState iState) throws IOException {
        final QueryField field = qState.queryField;
        Query q = isNullState.queryFactory.create(field.fieldName);
        qState.searchMode.execute(iState.searcher, q, field.sort, TOP_K);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(SqlOperatorOnIntBenchmark.class.getSimpleName())
                .forks(1).build();
        new Runner(opt).run();
    }
}
