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
import org.openjdk.jmh.annotations.AuxCounters.Type;
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

    private static int recordCounter = 0;

    public static final class Record {
        private List<Comparable> fields;
        private int recordIndex = 0;

        public Record(List<Comparable> fields) {
            this.fields = Collections.unmodifiableList(fields);
            recordIndex = recordCounter++;
        }

        @Override
        public String toString() {
            return fields.toString() + "@" + recordIndex;
        }

        @Override
        public int hashCode() {
            return fields.hashCode();
        }
    }

    public enum FieldType {
        INT, DATE
    }

    /**
     * Calculates a checksum over the result so that it can be verified. It forces
     * the implementations to iterate once in-order over the result.
     */
    private static class Checksum {
        long h = 0;
        long hStable = 0;

        public void add(Object obj) {
            h = (h << 2) ^ obj.hashCode();
            hStable = ((hStable << 2) ^ obj.hashCode()) << 2 + ((Record) obj).recordIndex;
//            System.out.println("  " + obj + "  " + h);
        }

        public long get() {
            return h == 0 ? 1 : h;
        }

        public static Checksum of(Iterable<?> iterable) {
            Checksum c = new Checksum();
            for (Object r : iterable) {
                c.add(r);
            }
            return c;
        }

        public static Checksum of(Object[] array) {
            Checksum c = new Checksum();
            for (Object r : array) {
                c.add(r);
            }
            return c;
        }
    }

    @State(Scope.Thread)
    @AuxCounters(Type.EVENTS)
    public static class Counters {
        public long comparisons;
        public int stableSort;
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

        public Comparator<Record> comparator;

        private List<Record> data;
        private Comparator<Record> internalComparator;
        private Checksum checksum = null;
        private Checksum proposedChecksum = null;
        private long cmpCalls = 0;

        /** Has to be submitted, otherwise the result will not be accepted */
        Checksum submitChecksum(Checksum c, Counters cmpCount) {
            proposedChecksum = c;
            if (cmpCount != null) {
                cmpCount.comparisons = cmpCalls;
                cmpCount.stableSort = 0;
                if (proposedChecksum != null)
                    cmpCount.stableSort = (proposedChecksum.hStable == checksum.hStable) ? 1 : 0;
            }
            this.cmpCalls = 0;
            return c;
        }

        @Setup(Level.Trial)
        public void setup() {
            data = new ArrayList<>(tupleNumber);
            Random rand = new Random(22);
            // make sure that some tuples with compare(a,b) == 0 exist,
            // to check whether the algorithm handles all cases correctly
            int maxInt = Math.max(3, (int) Math.pow(tupleNumber, 1. / this.fieldsNumber));
            recordCounter = 0;
            for (int i = 0; i < tupleNumber; i++) {
                List<Comparable> fields = new ArrayList<>(this.fieldsNumber);
                for (int j = 0; j < this.fieldsNumber; j++) {
                    Comparable<?> value = null;
                    switch (fieldType) {
                        case INT:
                            value = rand.nextInt(maxInt);
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
            internalComparator = all;
            comparator = (a, b) -> {
                this.cmpCalls++;
                return internalComparator.compare(a, b);
            };

            // calculate a checksum for the expected result
            List<Record> sorted = new ArrayList<>(data);
            sorted.sort(internalComparator);
            int resultSize = limit < 0 ? Integer.MAX_VALUE : limit;
            resultSize = Math.min(resultSize, sorted.size());
            checksum = Checksum.of(sorted.subList(0, resultSize));
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            this.cmpCalls = 0;
        }

        @TearDown(Level.Iteration)
        public void verifyChecksum() {
            if (checksum.h != proposedChecksum.h) {
                throw new IllegalStateException("checksum was " + proposedChecksum + " should have been " + checksum);
            }
            proposedChecksum = null;
        }

    }

    public static Object ignoreTrial() {
        throw new RuntimeException("ignore trial");
    }

    private void treeMapRemoveLastEntry(TreeMap<Record, List<Record>> map) {
        List<Record> l = map.get(map.lastKey());
        l.remove(l.size() - 1);
        if (l.isEmpty())
            map.remove(map.lastKey());
    }

    private void treeMapAddEntry(TreeMap<Record, List<Record>> map, Record i) {
        map.computeIfAbsent(i, k -> new ArrayList<>(1)).add(i);
    }

    @Benchmark
    public Object treeMap(QueryState iState, Counters cmpCount) {
        TreeMap<Record, List<Record>> map = new TreeMap<>(iState.comparator);
        long size = 0;
        for (Record i : iState.data) {
            treeMapAddEntry(map, i);
            size++;
            if (iState.limit > 0 && size > iState.limit) {
                treeMapRemoveLastEntry(map);
                size--;
            }
        }
        Checksum c = new Checksum();
        map.forEach((k, v) -> v.forEach(c::add));
        return iState.submitChecksum(c, cmpCount);
    }

    @Benchmark
    public Object treeMap2(QueryState iState, Counters cmpCount) {
        TreeMap<Record, List<Record>> map = new TreeMap<>(iState.comparator);
        long size = 0;
        for (Record i : iState.data) {
            if (iState.limit > 0 && size >= iState.limit) {
                if (iState.comparator.compare(i, map.lastKey()) >= 0)
                    continue;
                treeMapRemoveLastEntry(map);
                size--;
            }
            treeMapAddEntry(map, i);
            size++;
        }
        Checksum c = new Checksum();
        map.forEach((k, v) -> v.forEach(c::add));
        return iState.submitChecksum(c, cmpCount);
    }

    @Benchmark
    public Object collectionSort(QueryState iState, Counters cmpCount) {
        if (iState.limit >= 0 && iState.limit < iState.tupleNumber)
            return ignoreTrial();
        List<Record> list = new ArrayList<>(iState.data);
        Collections.sort(list, iState.comparator);
        return iState.submitChecksum(Checksum.of(list), cmpCount);
    }

    @Benchmark
    public Object priorityQueue(QueryState iState, Counters cmpCount) {
        PriorityQueue<Record> pq = new PriorityQueue<>(iState.comparator.reversed());
        for (Record i : iState.data) {
            if (iState.limit == -1) {
                pq.add(i);
            } else {
                if (pq.size() < iState.limit) {
                    pq.add(i);
                } else {
                    if (iState.comparator.compare(pq.peek(), i) > 0) {
                        pq.poll();
                        pq.add(i);
                    }
                }
            }
        }
        Record[] arr = pq.toArray(new Record[iState.limit < 0 ? pq.size() : iState.limit]);
        Arrays.sort(arr, iState.comparator);
        return iState.submitChecksum(Checksum.of(arr), cmpCount);
    }

    @Benchmark
    public Object array(QueryState state, Counters cmpCount) {
        if (state.limit < 0) {
            return ignoreTrial();
        }
        int limit = state.limit < 0 ? Integer.MAX_VALUE : state.limit;
        Comparator<Record> cmp = (a, b) -> {
            int c = state.comparator.compare(a, b);
            return c == 0 ? -1 : c;
        };
        List<Record> list = new ArrayList<>();
        for (Record i : state.data) {
            if (list.size() >= limit) {
                Record last = list.get(list.size() - 1);
                if (cmp.compare(i, last) > 0)
                    continue;
                int index = Collections.binarySearch(list, i, cmp);
                if (index < 0) {
                    list.add(-index - 1, i);
                    list.remove(list.size() - 1);
                }
            } else {
                int index = Collections.binarySearch(list, i, cmp);
                if (index < 0)
                    list.add(-index - 1, i);
            }
        }
        return state.submitChecksum(Checksum.of(list), cmpCount);
    }

    @Benchmark
    public Object topnHeap(QueryState iState, Counters cmpCount) {
        TopNHeap<Record, Record> min = new TopNHeap<>(r -> r, iState.comparator,
                iState.limit < 0 ? Integer.MAX_VALUE : iState.limit, 0);
        for (Record i : iState.data) {
            min.offer(i);
        }
        return iState.submitChecksum(Checksum.of(min.getResult()), cmpCount);
    }

    @Benchmark
    public Object topnHeap2(QueryState iState, Counters cmpCount) {
        TopNHeap2<Record, Record> min = new TopNHeap2<>(r -> r, iState.comparator,
                iState.limit < 0 ? Integer.MAX_VALUE : iState.limit, 0);
        for (Record i : iState.data) {
            min.offer(i);
        }
        return iState.submitChecksum(Checksum.of(min.getResult()), cmpCount);
    }

    @Benchmark
    public Object limitSort(QueryState iState, Counters cmpCount) {
        LimitSort<Record> min = new LimitSort<>(iState.comparator, iState.limit);
        for (Record i : iState.data) {
            min.offer(i);
        }
        return iState.submitChecksum(Checksum.of(min.getResult()), cmpCount);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(PartialSortBenchmark.class.getSimpleName()).forks(1).build();
        new Runner(opt).run();
    }
}
