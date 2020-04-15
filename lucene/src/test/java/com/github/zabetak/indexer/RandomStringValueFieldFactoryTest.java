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
import org.apache.lucene.document.StringField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RandomStringValueFieldFactoryTest {

    @Test
    public void testStringFieldSingleCreate() {
        StringField field = new StringField("field", "value", Field.Store.YES);
        RandomStringValueFieldFactory factory = RandomStringValueFieldFactory.of(field, 5);

        Field result = factory.create(new IndexGenerator.IterationContext(1));
        assertEquals(5, result.stringValue().length());
        assertEquals('1', result.stringValue().charAt(0));
    }

    @Test
    public void testStringFieldDoubleCreate() {
        StringField field = new StringField("field", "value", Field.Store.YES);
        RandomStringValueFieldFactory factory = RandomStringValueFieldFactory.of(field, 5);

        String first = factory.create(new IndexGenerator.IterationContext(1)).stringValue();
        assertEquals(5, first.length());
        assertEquals('1', first.charAt(0));

        String second = factory.create(new IndexGenerator.IterationContext(2)).stringValue();
        assertEquals(5, second.length());
        assertEquals('2', second.charAt(0));

        assertNotEquals(first.substring(1), second.substring(1), "Value suffixes should be different");
    }

    @Test
    public void testContextDocIdGreaterThanValueLen() {
        StringField field = new StringField("field", "value", Field.Store.YES);
        RandomStringValueFieldFactory factory = RandomStringValueFieldFactory.of(field, 5);

        Field result = factory.create(new IndexGenerator.IterationContext(123456));
        assertEquals("12345", result.stringValue());
    }

    @Test
    public void testReproducibleStringValues() {
        StringField field1 = new StringField("field1", "value1", Field.Store.YES);
        RandomStringValueFieldFactory factory1 = RandomStringValueFieldFactory.of(field1, 5);
        String value1 = factory1.create(new IndexGenerator.IterationContext(1)).stringValue();

        StringField field2 = new StringField("field2", "value2", Field.Store.YES);
        RandomStringValueFieldFactory factory2 = RandomStringValueFieldFactory.of(field2, 5);
        String value2 = factory2.create(new IndexGenerator.IterationContext(2)).stringValue();

        assertEquals(value1.substring(1), value2.substring(1));
    }

}
