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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * A generator for Lucene indexes.
 */
public final class IndexGenerator {

    /**
     * A context providing access to values of the current iteration.
     */
    public static final class IterationContext {
        private final int docId;

        public IterationContext(int docId) {
            this.docId = docId;
        }

        /**
         * Returns the id of the document that is about to be created.
         */
        public int docId() {
            return docId;
        }
    }

    /**
     * The seed is hardcoded to produce reproducible results.
     */
    private final Random RAND_NULLS = new Random(23);

    private Path indexPath;
    private int docNumber = 0;
    private List<FieldFactory> fieldFactories = new ArrayList<>();
    private int nullPercent = 0;
    private int commitThreshold = 100000;
    private boolean override = true;

    public void setDocNumber(int docNumber) {
        if (docNumber < 0)
            throw new IllegalArgumentException("Number of documents (" + docNumber + ") cannot be negative.");
        this.docNumber = docNumber;
    }

    public void setIndexPath(Path indexPath) {
        this.indexPath = Objects.requireNonNull(indexPath);
    }

    public void setNullPercent(int nullPercent) {
        if (nullPercent < 0 || nullPercent > 100)
            throw new IllegalArgumentException("Null percent (" + nullPercent + ") must be a value in the range [0, 100].");
        this.nullPercent = nullPercent;
    }

    public void setCommitThreshold(int commitThreshold) {
        this.commitThreshold = commitThreshold;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public void addFieldFactory(FieldFactory fieldFactory) {
        this.fieldFactories.add(Objects.requireNonNull(fieldFactory));
    }

    public void createIndex() throws IOException {
        if (!override && Files.exists(indexPath))
            return;
        try (Directory dir = FSDirectory.open(indexPath)) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            iwc.setMergePolicy(NoMergePolicy.INSTANCE);
            iwc.setCommitOnClose(true);
            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                for (int i = 0; i < docNumber; i++) {
                    Document doc = new Document();
                    // TODO Configure null frequency per field
                    if (RAND_NULLS.nextInt(100 + 1) > nullPercent) {
                        IterationContext context = new IterationContext(i);
                        for (FieldFactory fieldFactory : fieldFactories)
                            doc.add(fieldFactory.create(context));
                    }
                    writer.addDocument(doc);
                    if (i % commitThreshold == 0)
                        writer.commit();
                }
            }
        }
    }
}
