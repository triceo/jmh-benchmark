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

import java.io.File;
import java.util.List;
import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;
import org.optaplanner.examples.cloudbalancing.persistence.CloudBalanceXmlSolutionFileIO;

@State(Scope.Benchmark)
@Fork(jvmArgs = {"-Xms2G", "-Xmx2G"})
@BenchmarkMode(Mode.Throughput)
public class MyBenchmark {

    private static final Random RANDOM = new Random();
    static final SolutionDescriptor<CloudBalance> SOLUTION_DESCRIPTOR =
            SolutionDescriptor.buildSolutionDescriptor(CloudBalance.class, CloudProcess.class);

    static final CloudBalance FULL_SOLUTION = getSolution();

    @Param({"DRL", "CS-B", "CS-D"})
    public String algo;
    // This is a thin wrapper around KieSession.
    private Session session = null;

    private static CloudBalance getSolution() {
        CloudBalance originalSolution = new CloudBalanceXmlSolutionFileIO()
                .read(new File("1600computers-4800processes.xml"));
        List<CloudComputer> computers = originalSolution.getComputerList();
        // Initialize the solution randomly.
        for (CloudProcess cloudProcess : originalSolution.getProcessList()) {
            boolean initialize = (RANDOM.nextInt(100) > 5); // 95 % will be initialized.
            if (!initialize) {
                continue;
            }
            cloudProcess.setComputer(computers.get(RANDOM.nextInt(computers.size())));
        }
        return originalSolution;
    }

    private static Session getSession(String algo) {
        switch (algo) {
            case "CS-D":
                return new ConstraintStreamSession(ConstraintStreamImplType.DROOLS);
            case "CS-B":
                return new ConstraintStreamSession(ConstraintStreamImplType.BAVET);
            case "DRL":
                return new DroolsSession();
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Setup(Level.Invocation)
    public void setUp() {
        session = getSession(algo);
        // Insert all computer facts into the session. No need to be incremental.
        FULL_SOLUTION.getComputerList().forEach(session::insert);
    }

    @TearDown(Level.Invocation)
    public void tearDown() {
        session.close();
        session = null;
    }

    @Benchmark
    public Blackhole incremental(Blackhole bh) {
        for (CloudProcess process : FULL_SOLUTION.getProcessList()) {
            bh.consume(session.insert(process));
            bh.consume(session.calculateScore());
        }
        return bh;
    }

    @Benchmark
    public Blackhole allAtOnce(Blackhole bh) {
        for (CloudProcess process : FULL_SOLUTION.getProcessList()) {
            bh.consume(session.insert(process));
        }
        bh.consume(session.calculateScore());
        return bh;
    }
}
