package org.example;

import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.impl.score.director.stream.ConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.impl.score.stream.ConstraintSession;
import org.optaplanner.examples.rocktour.domain.RockShow;
import org.optaplanner.examples.rocktour.domain.RockTourSolution;

import static org.optaplanner.examples.rocktour.domain.RockTourConstraintConfiguration.UNASSIGNED_SHOW;

final class ConstraintStreamSession implements Session {

    private final ConstraintSession<RockTourSolution, HardMediumSoftLongScore> session;

    private static Constraint getConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.fromUnfiltered(RockShow.class)
                .filter(rockShow -> rockShow.getDate() == null)
                .filter(rockShow -> rockShow.getBus() != null)
                .penalizeConfigurable(UNASSIGNED_SHOW);
    }

    public ConstraintStreamSession(ConstraintStreamImplType implType, RockTourSolution solution) {
        ConstraintProvider constraints = constraintFactory -> new Constraint[]{
                getConstraint(constraintFactory)
        };
        ConstraintStreamScoreDirectorFactory<RockTourSolution, ?> scoreDirectorFactory =
                new ConstraintStreamScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, constraints, implType);
        session = (ConstraintSession<RockTourSolution, HardMediumSoftLongScore>) scoreDirectorFactory
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
    public HardMediumSoftLongScore calculateScore() {
        return session.calculateScore(0);
    }

    @Override
    public void close() {
        session.close();
    }
}
