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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A builder for creating paths for Lucene indexes in a systematic way.
 */
public final class IndexPathBuilder {

    private Path root = Paths.get(System.getProperty("java.io.tmpdir"), "jmh-lucene-indexes");
    private String className;
    private int docNumber = 0;
    private int stringLength = 0;
    private int nullPercent = -1;

    public IndexPathBuilder(Class<?> clazz) {
        this.className = clazz.getSimpleName();
    }

    public void setDocNumber(int docNumber) {
        if (docNumber <= 0)
            throw new IllegalArgumentException("Document number (" + docNumber + ") must be greater than 0.");
        this.docNumber = docNumber;
    }

    public void setStringLength(int stringLength) {
        if (stringLength <= 0)
            throw new IllegalArgumentException("String length (" + stringLength + ") must be greater than 0.");
        this.stringLength = stringLength;
    }

    public void setNullPercent(int nullPercent) {
        if (nullPercent < 0 || nullPercent > 100)
            throw new IllegalArgumentException("Null percentage (" + nullPercent + ") must be in the range [0, 100].");
        this.nullPercent = nullPercent;
    }

    public Path build() {
        Path b = root;
        b = b.resolve(className);
        b = b.resolve(docNumber + "-docs");
        b = stringLength > 0 ? b.resolve(stringLength + "-str-len") : b;
        b = nullPercent >= 0 ? b.resolve(nullPercent + "-null-percent") : b;
        return b.resolve("index");
    }
}
