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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RandomAsciiStringTest {

    @Test
    public void testReproducibleValues() {
        RandomAsciiString random = new RandomAsciiString(11, 20);
        List<String> expected = Arrays.asList(
                "canDtbqZx^aKld_]eHYY",
                "D`foboxcSZetiLPkgdCi",
                "EVor]aUGXa\\sqePH^yLD",
                "TrdepSEVRzJyp[\\yQAGb",
                "OiRtlCvWtGeVL_p`EPHQ");
        List<String> actual = new ArrayList<>(5);
        for (int i = 0; i < 5; i++)
            actual.add(random.next());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testStringCharsInValidRange() {
        RandomAsciiString random = new RandomAsciiString(11, 5);
        for (int k = 0; k < 100; k++) {
            String val = random.next();
            for (int i = 0; i < val.length(); i++) {
                char c = val.charAt(i);
                assertThat(c,
                        is(both(greaterThanOrEqualTo(RandomAsciiString.LOWER_BOUND)).and(lessThanOrEqualTo(RandomAsciiString.UPPER_BOUND))));
            }
        }
    }

    @Test
    public void testStringHasCorrectLength() {
        for (int len = 0; len < 5; len++) {
            RandomAsciiString random = new RandomAsciiString(11, len);
            for (int i = 0; i < 10; i++) {
                String val = random.next();
                assertThat(val.length(), is(len));
            }
        }
    }

}
