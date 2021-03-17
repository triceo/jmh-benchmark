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
import org.optaplanner.examples.rocktour.domain.RockShow;
import org.optaplanner.examples.rocktour.domain.RockStandstill;
import org.optaplanner.examples.rocktour.domain.RockTourSolution;
import org.optaplanner.examples.rocktour.persistence.RockTourXlsxFileIO;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Fork(value = 5, jvmArgs = {"-Xms4G", "-Xmx4G"})
@BenchmarkMode(Mode.Throughput)
public class MyBenchmark {

    public static final Random RANDOM = new Random(0L);
    static final SolutionDescriptor<RockTourSolution> SOLUTION_DESCRIPTOR =
            SolutionDescriptor.buildSolutionDescriptor(RockTourSolution.class, RockShow.class, RockStandstill.class);

    @Param({"CS-B", "CS-D"})
    public String algo;

    private RockTourSolution solution = null;
    private Session session = null;
    private RockShow fact1 = null;
    private RockShow fact2 = null;

    private static RockTourSolution readSolution() {
        RockTourXlsxFileIO fileIo = new RockTourXlsxFileIO();
        return fileIo.read(new File("47shows.xlsx"));
    }

    private static Session getSession(String algo, RockTourSolution solution) {
        switch (algo) {
            case "CS-D":
                return new ConstraintStreamSession(ConstraintStreamImplType.DROOLS, solution);
            case "CS-B":
                return new ConstraintStreamSession(ConstraintStreamImplType.BAVET, solution);
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Setup(Level.Trial)
    public void setUp() {
        solution = readSolution();
        session = getSession(algo, solution);
        // Insert all facts into the session. No need to be incremental.
        solution.getShowList().forEach(session::insert);
        session.insert(solution.getBus());
        // Make sure everything is primed.
        session.calculateScore();
    }

    @Setup(Level.Invocation)
    public void pickFacts() {
        // Pick two random facts to benchmark.
        // This simulates OptaPlanner which would also choose the facts mostly at random.
        List<RockShow> allFacts = solution.getShowList();
        int factCount = allFacts.size();
        fact1 = allFacts.get(RANDOM.nextInt(factCount));
        do {
            fact2 = allFacts.get(RANDOM.nextInt(factCount));
        } while (Objects.equals(fact1, fact2));
    }

    @TearDown(Level.Invocation)
    public void removeFacts() {
        fact1 = null;
        fact2 = null;
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        session.close();
        session = null;
    }

    @Benchmark
    public Blackhole swapTwo(Blackhole bh) {
        RockStandstill oldValue = fact1.getPreviousStandstill();
        fact1.setPreviousStandstill(fact2.getPreviousStandstill());
        fact2.setPreviousStandstill(oldValue);
        bh.consume(session.update(fact1));
        bh.consume(session.update(fact2));
        bh.consume(session.calculateScore());
        return bh;
    }

}
