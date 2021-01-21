package org.example.domain;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@PlanningSolution
public final class MySolution {

    public static MySolution generate(int factCount, int joinRatio) {
        if (joinRatio < 1 || joinRatio > 100) {
            throw new IllegalStateException("Invalid join ratio: " + joinRatio);
        }
        int idsAvailable = (int) Math.round(100.0 / joinRatio);
        Random random = new Random();
        List<MyFact> facts = IntStream.range(0, factCount)
                .mapToObj(id -> new MyFact(id, random.nextInt(idsAvailable)))
                .collect(Collectors.toList());
        return new MySolution(facts);
    }

    private final List<MyFact> facts;
    private SimpleScore score;

    public MySolution(List<MyFact> facts) {
        this.facts = facts;
    }

    @PlanningEntityCollectionProperty
    public List<MyFact> getFacts() {
        return facts;
    }

    @ProblemFactCollectionProperty
    public List<String> getNothing() {
        return Collections.emptyList(); // Required for some reason.
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(final SimpleScore score) {
        this.score = score;
    }
}
