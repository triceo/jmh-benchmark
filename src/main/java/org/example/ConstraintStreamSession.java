package org.example;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.impl.score.director.stream.ConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.impl.score.stream.ConstraintSession;
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;

final class ConstraintStreamSession implements Session {

    private static final ConstraintProvider CONSTRAINT_PROVIDER = constraintFactory -> new Constraint[]{
            constraintFactory.from(CloudProcess.class)
                    .groupBy(CloudProcess::getComputer, ConstraintCollectors.sum(CloudProcess::getRequiredCpuPower))
                    .filter((computer, requiredCpuPower) -> requiredCpuPower > computer.getCpuPower())
                    .penalize("requiredCpuPowerTotal", HardSoftScore.ONE_HARD,
                    (computer, requiredCpuPower) -> requiredCpuPower - computer.getCpuPower())
    };
    private static final ConstraintStreamScoreDirectorFactory<CloudBalance, ?> CSB_SDF =
            new ConstraintStreamScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, CONSTRAINT_PROVIDER,
                    ConstraintStreamImplType.BAVET);
    private static final ConstraintStreamScoreDirectorFactory<CloudBalance, ?> CSD_SDF =
            new ConstraintStreamScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, CONSTRAINT_PROVIDER,
                    ConstraintStreamImplType.DROOLS);
    private final ConstraintSession<CloudBalance, HardSoftScore> session;

    public ConstraintStreamSession(ConstraintStreamImplType constraintStreamImplType) {
        ConstraintStreamScoreDirectorFactory<CloudBalance, ?> scoreDirectorFactory =
                constraintStreamImplType == ConstraintStreamImplType.DROOLS ? CSD_SDF : CSB_SDF;
        session = (ConstraintSession<CloudBalance, HardSoftScore>) scoreDirectorFactory
                .newConstraintStreamingSession(false, MyBenchmark.FULL_SOLUTION);
    }

    @Override
    public int insert(Object object) {
        session.insert(object);
        return 0;
    }

    @Override
    public HardSoftScore calculateScore() {
        return session.calculateScore(0);
    }

    @Override
    public void close() {
        session.close();
    }
}
