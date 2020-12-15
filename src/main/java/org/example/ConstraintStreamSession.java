package org.example;

import java.util.function.Function;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.impl.score.director.stream.ConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.impl.score.stream.ConstraintSession;
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;

final class ConstraintStreamSession implements Session {

    private static final ConstraintProvider CONSTRAINT_PROVIDER = constraintFactory -> new Constraint[]{
            constraintFactory.fromUnfiltered(CloudComputer.class)
                    .join(CloudProcess.class, Joiners.equal(Function.identity(), CloudProcess::getComputer))
                    .penalize("requiredCpuPowerTotal", HardSoftScore.ONE_HARD)
    }; // Scoring is still fully enabled, so the comparison isn't entirely fair.
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
    public int update(Object object) {
        session.update(object);
        return 1;
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
