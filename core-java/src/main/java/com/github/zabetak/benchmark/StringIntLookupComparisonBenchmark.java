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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * A benchmark comparing lookup in (hash-based) sets of an int with different
 * representations (strings and integers).
 *
 * TODO: WIP Don't take into consideration yet
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Timeout(time = 1, timeUnit = TimeUnit.MINUTES)
@Fork(1)
public class StringIntLookupComparisonBenchmark {

	@State(Scope.Benchmark)
	public static class PoolState {
		@Param({ "10000000" })
		public int poolSize;

		private Integer[] intPool;
		private String[] stringPool;

		@Setup(Level.Trial)
		public void setup() throws IOException {

			intPool = new Integer[poolSize];
			stringPool = new String[poolSize];
			for (int i = 0; i < poolSize; i++) {
				intPool[i] = i;
				stringPool[i] = String.valueOf(i);
			}
		}
	}

	@State(Scope.Benchmark)
	public static class SetState {
		@Param({ "10000000" })
		public int totalLookups;

		@Param({ "10000000" })
		public int setSize;
		private Set<Integer> intSet;
		private Set<String> strSet;

		
		@Setup(Level.Trial)
		public void setup() throws IOException {
			intSet = new HashSet<>(setSize);
			strSet = new HashSet<>(setSize);

			for (int i = 0; i < setSize; i++) {
				intSet.add(i);
				strSet.add(String.valueOf(i));
			}
		}

	}

	@Benchmark
	public void setContainsWithInt(SetState is, PoolState valuePool, Blackhole bh) {
		for (int i = 0; i < is.totalLookups; i++) {
			Integer lookupValue = valuePool.intPool[i % valuePool.poolSize];
			bh.consume(is.intSet.contains(lookupValue));
		}
	}

	@Benchmark
	public void buildSetWithInts(SetState is, PoolState valuePool) {
		Set<Integer> set = new HashSet<>();
		for (int i = 0; i < is.setSize; i++) {
			set.add(valuePool.intPool[i]);
		}
	}

	@Benchmark
	public void buildSetWithStrings(SetState is, PoolState valuePool) {
		Set<String> set = new HashSet<>();
		for (int i = 0; i < is.setSize; i++) {
			set.add(valuePool.stringPool[i]);
		}
	}

	@Benchmark
	public void setContainsWithString(SetState is, PoolState valuePool, Blackhole bh) {
		for (int i = 0; i < is.totalLookups; i++) {
			String lookupValue = valuePool.stringPool[i % valuePool.poolSize];
			bh.consume(is.strSet.contains(lookupValue));
		}
	}


	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include(StringIntLookupComparisonBenchmark.class.getSimpleName()).forks(1).build();
		new Runner(opt).run();
	}
}
