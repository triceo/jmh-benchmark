package org.example;

import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

interface Session {

    int insert(Object object);

    int update(Object object);

    HardMediumSoftLongScore calculateScore();

    void close();
}
