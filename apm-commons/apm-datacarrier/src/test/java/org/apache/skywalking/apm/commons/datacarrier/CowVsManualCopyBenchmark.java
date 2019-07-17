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
 *
 */

package org.apache.skywalking.apm.commons.datacarrier;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author kezhenxu94
 */
@State(Scope.Thread)
public class CowVsManualCopyBenchmark {
    @Param({"10", "100", "1000"})
    private int n;

    @Benchmark
    public void testCow() {
        List<Object> list = new CopyOnWriteArrayList<Object>();
        for (int i = 0; i < n; i++) {
            list.add(new Object());
        }
    }

    @Benchmark
    public void testManuallyCopy() {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < n; i++) {
            List<Object> newList = new ArrayList<Object>(list);
            newList.add(new Object());
            list = newList;
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(CowVsManualCopyBenchmark.class.getSimpleName())
            .mode(Mode.Throughput)
            .forks(1)
            .warmupIterations(4)
            .measurementIterations(4)
            .build();

        new Runner(opt).run();
    }
}
/*
 * Benchmark                                   (n)   Mode  Cnt        Score        Error  Units
 * CowVsManualCopyBenchmark.testCow             10  thrpt    4  2133748.077 ± 129244.049  ops/s
 * CowVsManualCopyBenchmark.testCow            100  thrpt    4   147487.152 ± 216138.079  ops/s
 * CowVsManualCopyBenchmark.testCow           1000  thrpt    4     2800.961 ±   3252.492  ops/s
 * CowVsManualCopyBenchmark.testManuallyCopy    10  thrpt    4  1590487.819 ± 832615.119  ops/s
 * CowVsManualCopyBenchmark.testManuallyCopy   100  thrpt    4   106537.480 ±   8958.785  ops/s
 * CowVsManualCopyBenchmark.testManuallyCopy  1000  thrpt    4     1422.433 ±    124.016  ops/s
 */
