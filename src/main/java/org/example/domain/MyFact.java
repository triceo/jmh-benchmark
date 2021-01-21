package org.example.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PlanningEntity
public final class MyFact {

    @PlanningId
    private final long id;
    private final long joinId;

    @PlanningVariable(valueRangeProviderRefs = "values")
    private String variable = UUID.randomUUID().toString();

    @ValueRangeProvider(id = "values")
    private final Set<String> valueRange = Stream.generate(() -> UUID.randomUUID().toString())
            .limit(3)
            .collect(Collectors.toSet());

    public MyFact(final long id, final long joinId) {
        this.id = id;
        this.joinId = joinId;
    }

    public long getId() {
        return id;
    }

    public long getJoinId() {
        return joinId;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(final String variable) {
        this.variable = variable;
    }

    public Set<String> getValueRange() {
        return valueRange;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || !Objects.equals(getClass(), o.getClass())) return false;
        final MyFact fact = (MyFact) o;
        return id == fact.id && Objects.equals(variable, fact.variable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
