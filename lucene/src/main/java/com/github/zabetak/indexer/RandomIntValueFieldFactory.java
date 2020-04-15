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
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;

import java.util.Random;

public abstract class RandomIntValueFieldFactory implements FieldFactory {
    private final Random random;
    private final Field field;
    private final int lowerBound;
    private final int upperBound;

    public static RandomIntValueFieldFactory withPoint(IntPoint field, int lowerBound,
                                                       int upperBound) {
        return new RandomIntValueFieldFactory(field, lowerBound, upperBound) {
            @Override
            protected void setFieldValue(Field field, int value) {
                field.setIntValue(value);
            }
        };
    }

    public static RandomIntValueFieldFactory withStored(StoredField field, int lowerBound,
                                                        int upperBound) {
        return new RandomIntValueFieldFactory(field, lowerBound, upperBound) {
            @Override
            protected void setFieldValue(Field field, int value) {
                field.setIntValue(value);
            }
        };
    }

    public static RandomIntValueFieldFactory withDocValues(NumericDocValuesField field,
                                                           int lowerBound, int upperBound) {
        return new RandomIntValueFieldFactory(field, lowerBound, upperBound) {
            @Override
            protected void setFieldValue(Field field, int value) {
                field.setLongValue(value);
            }
        };
    }

    private RandomIntValueFieldFactory(Field field, int lowerBound, int upperBound) {
        if (lowerBound < 0)
            throw new IllegalArgumentException("Lower bound (" + lowerBound + ") must be greater " +
                    "or equal to 0.");
        if (upperBound <= lowerBound)
            throw new IllegalArgumentException("Upper bound (" + upperBound + ") must be " +
                    "greater than the lower bound (" + lowerBound + ").");
        this.random = new Random(23);
        this.field = field;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public final Field create(IndexGenerator.IterationContext valueContext) {
        int value = lowerBound + random.nextInt(upperBound - lowerBound);
        setFieldValue(field, value);
        return field;
    }

    protected abstract void setFieldValue(Field field, int value);
}
