/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.example;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;

import java.util.List;
import java.util.Random;

@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 20)
@Fork(value = 5, jvmArgs = {"-Xms4G", "-Xmx4G"})
@BenchmarkMode(Mode.Throughput)
public class MyBenchmark {

    private static final Random RANDOM = new Random();
    static final SolutionDescriptor<MySolution> SOLUTION_DESCRIPTOR =
            SolutionDescriptor.buildSolutionDescriptor(MySolution.class, MyFact.class);

    static final MySolution SOLUTION_100 = MySolution.generate(100);
    static final MySolution SOLUTION_1K = MySolution.generate(1000);
    static final MySolution SOLUTION_10K = MySolution.generate(10000);

    @Param({"DRL", "CS-B"})
    public String algo;

    @Param({"100", "1000", "10000"})
    public int size;

    // This is a thin wrapper around KieSession.
    private Session session = null;
    private MyFact fact1 = null;
    private MyFact fact2 = null;

    private static Session getSession(String algo, MySolution solution) {
        switch (algo) {
            case "CS-D":
                return new ConstraintStreamSession(ConstraintStreamImplType.DROOLS, solution);
            case "CS-B":
                return new ConstraintStreamSession(ConstraintStreamImplType.BAVET, solution);
            case "DRL":
                return new DrlSession();
            default:
                throw new UnsupportedOperationException();
        }
    }

    private MySolution getSolution() {
        switch (size) {
            case 100:
                return SOLUTION_100;
            case 1000:
                return SOLUTION_1K;
            case 10000:
                return SOLUTION_10K;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Setup(Level.Trial)
    public void setUp() {
        MySolution solution = getSolution();
        session = getSession(algo, solution);
        // Insert all facts into the session. No need to be incremental.
        List<MyFact> allFacts = solution.getFacts();
        allFacts.forEach(session::insert);
        session.calculateScore();
        // Pick two random processes to benchmark.
        int random = Math.max(1, RANDOM.nextInt(allFacts.size()));
        fact1 = allFacts.get(random);
        fact2 = allFacts.get(random - 1);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        session.close();
        session = null;
        fact1 = null;
        fact2 = null;
    }

    @Benchmark
    public Blackhole swap(Blackhole bh) {
        String oldValue = fact1.getVariable();
        fact1.setVariable(fact2.getVariable());
        fact2.setVariable(oldValue);
        bh.consume(session.update(fact1));
        bh.consume(session.update(fact2));
        bh.consume(session.calculateScore());

        oldValue = fact1.getVariable();
        fact1.setVariable(fact2.getVariable());
        fact2.setVariable(oldValue);
        bh.consume(session.update(fact1));
        bh.consume(session.update(fact2));
        bh.consume(session.calculateScore());

        return bh;
    }

    @Benchmark
    public Blackhole change(Blackhole bh) {
        String oldValue = fact1.getVariable();
        fact1.setVariable(fact2.getVariable());
        bh.consume(session.update(fact1));
        bh.consume(session.calculateScore());

        fact1.setVariable(oldValue);
        bh.consume(session.update(fact1));
        bh.consume(session.calculateScore());

        return bh;
    }
}
