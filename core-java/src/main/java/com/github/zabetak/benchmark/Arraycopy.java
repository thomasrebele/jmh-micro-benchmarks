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

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * TODO: WIP
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Timeout(time = 1, timeUnit = TimeUnit.MINUTES)
@Fork(1)
public class Arraycopy {

    private static class Record {

    }

    @State(Scope.Benchmark)
    public static class QueryState {

        @Param({"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"})
        public int toCopy;

        private Random rnd;
        private Object[] src, dst;

        @Setup(Level.Trial)
        public void setup() {
            rnd = new Random(22);
            int n = 200;
            src = new Object[n];
            dst = new Object[n];
            for (int i = 0; i < n; i++) {
                src[i] = new Record();
            }
        }

        public int fromIdx() {
            return rnd.nextInt(src.length - toCopy);
        }

        public int toIdx() {
            return rnd.nextInt(dst.length - toCopy);
        }
    }

    @Benchmark
    public Object arrayCopy(QueryState iState) {
        int fromIdx = iState.fromIdx();
        int toIdx = iState.toIdx();
        System.arraycopy(iState.src, fromIdx, iState.dst, toIdx, iState.toCopy);
        return iState.dst;
    }

    @Benchmark
    public Object loop(QueryState iState) {
        int fromIdxEnd = iState.fromIdx() + iState.toCopy;
        int toIdxEnd = iState.toIdx() + iState.toCopy;
        for (int i = iState.toCopy; i-- > 0;) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        }
        return iState.dst;
    }

    @Benchmark
    public Object manual(QueryState iState) {
        int fromIdxEnd = iState.fromIdx() + iState.toCopy;
        int toIdxEnd = iState.toIdx() + iState.toCopy;
        if (iState.toCopy == 0) {
        } else if (iState.toCopy == 1) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 2) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 3) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 4) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 5) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 6) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd]; // 5
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 7) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd]; // 5
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 8) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd]; // 5
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 9) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd]; // 5
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
        } else if (iState.toCopy == 10) {
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd]; // 5
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd]; // 10
        } else {
            throw new UnsupportedOperationException();
        }

        return iState.dst;
    }

    @Benchmark
    public Object manualSwitch(QueryState iState) {
        int fromIdxEnd = iState.fromIdx() + iState.toCopy;
        int toIdxEnd = iState.toIdx() + iState.toCopy;

        switch (iState.toCopy) {
            case 10:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 9:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 8:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 7:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 6:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 5:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 4:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 3:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 2:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 1:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 0:
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return iState.dst;
    }

    @Benchmark
    public Object combined1(QueryState iState) {
        int fromIdx = iState.fromIdx();
        int fromIdxEnd = fromIdx + iState.toCopy;
        int toIdx = iState.toIdx();
        int toIdxEnd = toIdx + iState.toCopy;

        switch (iState.toCopy) {
            case 5:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 4:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 3:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 2:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 1:
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            case 0:
                break;
            default:
                System.arraycopy(iState.src, fromIdx, iState.dst, toIdx, iState.toCopy);
        }
        return iState.dst;
    }

    @Benchmark
    public Object combined2(QueryState iState) {
        int fromIdx = iState.fromIdx();
        int fromIdxEnd = fromIdx + iState.toCopy;
        int toIdx = iState.toIdx();
        int toIdxEnd = toIdx + iState.toCopy;

        if (iState.toCopy <= 5) {
            for (int i = iState.toCopy; i-- > 0;) {
                iState.dst[--toIdxEnd] = iState.src[--fromIdxEnd];
            }
        } else {
            System.arraycopy(iState.src, fromIdx, iState.dst, toIdx, iState.toCopy);
        }
        return iState.dst;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(Arraycopy.class.getSimpleName()).forks(1).build();
        new Runner(opt).run();
    }
}
