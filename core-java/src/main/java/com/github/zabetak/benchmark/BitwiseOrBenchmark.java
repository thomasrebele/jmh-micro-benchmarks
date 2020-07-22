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
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A benchmark comparing bitwise OR operations among bitsets of various sizes.
 * <p>
 * The bitsets on which we perform the operations are selected from pre-initialised pools since it is infeasible to
 * create as many bitsets as the number of operations (when bitset size and operation number are big).
 * </p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
//@Fork(value = 1, jvmArgs = {"-Xmx16g", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly"})
@Fork(value = 1, jvmArgs = {"-Xmx16g"})
public class BitwiseOrBenchmark {

    @State(Scope.Benchmark)
    public static class InputState {
        @Param({"1000"})
        public int nBitsets;
    }

    @State(Scope.Benchmark)
    public static class BitSetPoolConfig {
        /**
         * The number of bits in each bitset
         */
        @Param({"436465696"})
        public int bitsetLength;

        /**
         * The size of the pool holding different bitsets.
         */
        @Param({"100"})
        public int bitsetPoolSize;

        /**
         * The seed used to fill the pool with random values.
         */
        public int poolSeed = 33;
    }

    @State(Scope.Benchmark)
    public static class ByteArrayPool {

        private byte[][] pool;
        private int entrySize;

        @Setup(Level.Trial)
        public void setup(BitSetPoolConfig config) {
            Random rand = new Random(config.poolSeed);
            entrySize = (int) Math.ceil(config.bitsetLength / 8.0);
            pool = new byte[config.bitsetPoolSize][entrySize];
            for (int i = 0; i < config.bitsetPoolSize; i++) {
                rand.nextBytes(pool[i]);
            }
        }

        public byte[] get(int index) {
            return pool[index % pool.length];
        }
    }

    @State(Scope.Benchmark)
    public static class BitSetPool {

        public BitSet[] pool;
        private int entrySize;

        @Setup(Level.Trial)
        public void setup(BitSetPoolConfig config) {
            Random rand = new Random(config.poolSeed);
            pool = new BitSet[config.bitsetPoolSize];
            entrySize = config.bitsetLength;
            byte[] tmp = new byte[(int) Math.ceil(config.bitsetLength / 8.0)];
            for (int i = 0; i < config.bitsetPoolSize; i++) {
                rand.nextBytes(tmp);
                pool[i] = BitSet.valueOf(tmp);
            }
        }

        public BitSet get(int index) {
            return pool[index % pool.length];
        }
    }

    @State(Scope.Benchmark)
    public static class BatchState {
        @Param({"8", "16", "32"})
        public int nBatches;

        public List<Batch> createBatches(int inputSize) {
            List<Batch> batches = new ArrayList<>(nBatches);
            int batchSize = Math.max(1, inputSize / nBatches);
            for (int p = 0; p < inputSize; p += batchSize) {
                int start = p;
                int end = Math.min(p + batchSize, inputSize);
                batches.add(new Batch(start, end));
            }
            return batches;
        }
    }

    @Benchmark
    public byte[] rowWiseWithByteArray(InputState oState, ByteArrayPool bitsetPool) {
        final int len = bitsetPool.entrySize;
        final byte[] result = new byte[len];
        for (int i = 0; i < oState.nBitsets; i++) {
            for (int j = 0; j < len; j++) {
                result[j] |= bitsetPool.get(i)[j];
            }
        }
        return result;
    }

    @Benchmark
    public byte[] columnWiseWithByteArray(InputState oState, ByteArrayPool bitsetPool) {
        final int len = bitsetPool.entrySize;
        final byte[] result = new byte[len];
        for (int j = 0; j < len; j++) {
            for (int i = 0; i < oState.nBitsets; i++) {
                result[j] |= bitsetPool.get(i)[j];
            }
        }
        return result;
    }

    @Benchmark
    public byte[] columnWiseBatchWithByteArray(InputState oState, ByteArrayPool bitsetPool, BatchState bState) {
        final int len = bitsetPool.entrySize;
        final byte[] result = new byte[len];
        for (Batch p : bState.createBatches(len)) {
            for (int i = 0; i < oState.nBitsets; i++) {
                for (int j = p.start; j < p.end; j++) {
                    result[j] |= bitsetPool.get(i)[j];
                }
            }
        }
        return result;
    }

    @Benchmark
    public byte[] rowWiseBatchWithByteArray(InputState oState, ByteArrayPool bitsetPool, BatchState bState) {
        final int len = bitsetPool.entrySize;
        final byte[] result = new byte[len];
        for (Batch p : bState.createBatches(oState.nBitsets)) {
            for (int i = p.start; i < p.end; i++) {
                for (int j = 0; j < len; j++) {
                    result[j] |= bitsetPool.get(i)[j];
                }
            }
        }
        return result;
    }

    @Benchmark
    public byte[] columnWiseBatchWithByteArrayInParallel(InputState oState, ByteArrayPool bitsetPool, BatchState bState) {
        final int len = bitsetPool.entrySize;
        ExecutorService service = Executors.newFixedThreadPool(bState.nBatches);
        final byte[] result = new byte[len];
        for (Batch p : bState.createBatches(len)) {
            service.submit(() -> {
                for (int i = 0; i < oState.nBitsets; i++) {
                    for (int j = p.start; j < p.end; j++) {
                        result[j] |= bitsetPool.get(i)[j];
                    }
                }
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(1, TimeUnit.MINUTES);
            return result;
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    @Benchmark
    public BitSet rowWiseWithBitSet(InputState oState, BitSetPool bitsetPool) {
        BitSet bitSet = new BitSet(bitsetPool.entrySize);
        for (int i = 0; i < oState.nBitsets; i++) {
            bitSet.or(bitsetPool.get(i));
        }
        return bitSet;
    }

    private static final class Batch {
        public final int start;
        public final int end;

        public Batch(int start, int end) {
            this.start = start;
            this.end = end;
        }

    }

    public static void main(String[] args) throws RunnerException, CommandLineOptionException {
        Options opt = new OptionsBuilder()
                .parent(new CommandLineOptions(args))
                .include(BitwiseOrBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
