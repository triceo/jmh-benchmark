package org.example;

import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.impl.score.director.stream.ConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.impl.score.stream.ConstraintSession;

final class ConstraintStreamSession implements Session {

    private static final ConstraintProvider CONSTRAINT_PROVIDER = constraintFactory -> new Constraint[]{
            constraintFactory.fromUnfiltered(MyFact.class)
                    .join(MyFact.class, Joiners.greaterThan(MyFact::getId))
                    .penalize("Join", SimpleScore.ONE)
    }; // Scoring is still fully enabled, so the comparison isn't entirely fair.
    private static final ConstraintStreamScoreDirectorFactory<MySolution, ?> CSB_SDF =
            new ConstraintStreamScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, CONSTRAINT_PROVIDER,
                    ConstraintStreamImplType.BAVET);
    private static final ConstraintStreamScoreDirectorFactory<MySolution, ?> CSD_SDF =
            new ConstraintStreamScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, CONSTRAINT_PROVIDER,
                    ConstraintStreamImplType.DROOLS);
    private final ConstraintSession<MySolution, SimpleScore> session;

    public ConstraintStreamSession(ConstraintStreamImplType constraintStreamImplType, MySolution solution) {
        ConstraintStreamScoreDirectorFactory<MySolution, ?> scoreDirectorFactory =
                constraintStreamImplType == ConstraintStreamImplType.DROOLS ? CSD_SDF : CSB_SDF;
        session = (ConstraintSession<MySolution, SimpleScore>) scoreDirectorFactory
                .newConstraintStreamingSession(false, solution);
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
    public SimpleScore calculateScore() {
        return session.calculateScore(0);
    }

    @Override
    public void close() {
        session.close();
    }
}
