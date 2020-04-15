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

public abstract class RandomStringValueFieldFactory implements FieldFactory {
    private final RandomAsciiString random;
    private final Field field;
    private final int valueLength;

    public static RandomStringValueFieldFactory of(StringField field, int valueLength) {
        return new RandomStringValueFieldFactory(field, valueLength) {
            @Override
            protected void setFieldValue(Field field, String value) {
                field.setStringValue(value);
            }
        };
    }

    public static RandomStringValueFieldFactory of(SortedDocValuesField field, int valueLength) {
        return new RandomStringValueFieldFactory(field, valueLength) {
            @Override
            protected void setFieldValue(Field field, String value) {
                field.setBytesValue(value.getBytes());
            }
        };
    }

    private RandomStringValueFieldFactory(Field field, int valueLength) {
        this.random = new RandomAsciiString(11, valueLength);
        this.field = field;
        this.valueLength = valueLength;
    }

    @Override
    public final Field create(IndexGenerator.IterationContext iterationContext) {
        String docIDPrefix = String.valueOf(iterationContext.docId());
        String randAscii = docIDPrefix + random.next();
        setFieldValue(field, randAscii.substring(0, valueLength));
        return field;
    }

    protected abstract void setFieldValue(Field field, String value);
}
