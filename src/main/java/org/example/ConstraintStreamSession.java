package org.example;

import org.example.domain.MyFact;
import org.example.domain.MySolution;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.stream.*;
import org.optaplanner.core.impl.score.director.stream.ConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.impl.score.stream.ConstraintSession;

final class ConstraintStreamSession implements Session {

    private final ConstraintSession<MySolution, SimpleScore> session;

    private static Constraint getConstraint(ConstraintFactory constraintFactory, int joinCount) {
        switch (joinCount) {
            case 1:
                return constraintFactory.fromUnfiltered(MyFact.class)
                        .join(constraintFactory.fromUnfiltered(MyFact.class),
                                Joiners.equal(MyFact::getJoinId))
                        .penalize("Join", SimpleScore.ONE);
            case 2:
                return constraintFactory.fromUnfiltered(MyFact.class)
                        .join(constraintFactory.fromUnfiltered(MyFact.class),
                                Joiners.lessThanOrEqual(MyFact::getJoinId, MyFact::getJoinId))
                        .join(constraintFactory.fromUnfiltered(MyFact.class),
                                Joiners.lessThanOrEqual((f1, f2) -> f2.getJoinId(), MyFact::getJoinId))
                        .penalize("Join", SimpleScore.ONE);
            case 3:
                return constraintFactory.fromUnfiltered(MyFact.class)
                        .join(constraintFactory.fromUnfiltered(MyFact.class),
                                Joiners.lessThanOrEqual(MyFact::getJoinId, MyFact::getJoinId))
                        .join(constraintFactory.fromUnfiltered(MyFact.class),
                                Joiners.lessThanOrEqual((f1, f2) -> f2.getJoinId(), MyFact::getJoinId))
                        .join(constraintFactory.fromUnfiltered(MyFact.class),
                                Joiners.lessThanOrEqual((f1, f2, f3) -> f3.getJoinId(), MyFact::getJoinId))
                        .penalize("Join", SimpleScore.ONE);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public ConstraintStreamSession(ConstraintStreamImplType implType, MySolution solution, int joinCount) {
        ConstraintProvider constraints = constraintFactory -> new Constraint[]{
                getConstraint(constraintFactory, joinCount)
        };
        ConstraintStreamScoreDirectorFactory<MySolution, ?> scoreDirectorFactory =
                new ConstraintStreamScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, constraints, implType);
        session = (ConstraintSession<MySolution, SimpleScore>) scoreDirectorFactory
                .newConstraintStreamingSession(false, solution);
    }

    @Override
    public int insert(Object object) {
        session.insert(object);
        return MyBenchmark.RANDOM.nextInt();
    }

    @Override
    public int update(Object object) {
        session.update(object);
        return MyBenchmark.RANDOM.nextInt();
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
