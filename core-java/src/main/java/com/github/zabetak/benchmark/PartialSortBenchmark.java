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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * TODO: WIP
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@Timeout(time = 1, timeUnit = TimeUnit.MINUTES)
@Fork(1)
public class PartialSortBenchmark {

    private static final Object VOID = new Object();

    private static final class Record {
        private List<Comparable> fields;

        public Record(List<Comparable> fields) {
            this.fields = Collections.unmodifiableList(fields);
        }
    }

    public enum FieldType {
        INT,
        DATE
    }

    @State(Scope.Benchmark)
    public static class QueryState {

        @Param({"1000000", "10000000"})
        public int tupleNumber;
        @Param({"-1", "10", "100"})
        public int limit;
        @Param({"2"})
        public int fieldsNumber;
        @Param("INT")
        public FieldType fieldType;

        private List<Record> data = new ArrayList<>();
        private Comparator<Record> comparator;

        @Setup(Level.Trial)
        public void setup() {
            Random rand = new Random(22);
            for (int i = 0; i < tupleNumber; i++) {
                List<Comparable> fields = new ArrayList<>(this.fieldsNumber);
                for (int j = 0; j < this.fieldsNumber; j++) {
                    Comparable<?> value = null;
                    switch (fieldType) {
                        case INT:
                            value = rand.nextInt();
                            break;
                        case DATE:
                            value = new Date(rand.nextLong());
                            break;
                    }
                    fields.add(value);
                }
                data.add(new Record(fields));
            }
            Comparator<Record> all = ((o1, o2) -> o1.fields.get(0).compareTo(o2.fields.get(0)));
            for (int i = 1; i < this.fieldsNumber; i++) {
                final int findex = i;
                all = all.thenComparing((o1, o2) -> o1.fields.get(findex).compareTo(o2.fields.get(findex)));
            }
            comparator = all;
        }
    }

    @Benchmark
    public Object treeMap(QueryState iState) {
        TreeMap<Record, Object> map = new TreeMap<>(iState.comparator);
        for (Record i : iState.data) {
            map.put(i, VOID);
            if (iState.limit > 0 && map.size() > iState.limit)
                map.remove(map.lastKey());
        }
        return map.values();
    }

    @Benchmark
    public Object treeMap2(QueryState iState) {
        TreeMap<Record, Object> map = new TreeMap<>(iState.comparator);
        for (Record i : iState.data) {

            if (iState.limit > 0 && map.size() > iState.limit) {
                if (iState.comparator.compare(i, map.lastKey()) > 0)
                    continue;
                map.pollLastEntry();
            }
            map.put(i, VOID);
        }
        return map.values();
    }

    @Benchmark
    public Object collectionSort(QueryState iState) {
        if (iState.limit >= 0)
            return null;
        List<Record> list = new ArrayList<>(iState.data);
        Collections.sort(list, iState.comparator);
        return list;
    }

    @Benchmark
    public Object priorityQueue(QueryState iState) {
        PriorityQueue<Record> min = new PriorityQueue<>(iState.comparator);
        for (Record i : iState.data) {
            if (iState.limit == -1) {
                min.add(i);
            } else {
                if (min.size() <= iState.limit)
                    min.add(i);
                else {
                    if (iState.comparator.compare(min.peek(), i) < 0) {
                        min.poll();
                        min.add(i);
                    }
                }
            }
        }
        Record[] arr = min.toArray(new Record[iState.limit < 0 ? min.size() : iState.limit]);
        Arrays.sort(arr, iState.comparator);
        return arr;
    }

    @Benchmark
    public Object array(QueryState state) {
        if (state.limit < 0) {
            return null;
        }
        List<Record> list = new ArrayList<>();
        for (Record i : state.data) {
            if (list.size() > state.limit) {
                Record last = list.get(list.size() - 1);
                if (state.comparator.compare(i, last) > 0)
                    continue;
                int index = Collections.binarySearch(list, i, state.comparator);
                if (index < 0) {
                    list.add(-index - 1, i);
                    list.remove(list.size() - 1);
                }
            } else {
                int index = Collections.binarySearch(list, i, state.comparator);
                if (index < 0)
                    list.add(-index - 1, i);
            }
        }
        return null;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(PartialSortBenchmark.class.getSimpleName())
                .forks(1).build();
        new Runner(opt).run();
    }
}
