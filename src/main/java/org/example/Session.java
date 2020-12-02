package org.example;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

interface Session {

    int insert(Object object);

    HardSoftScore calculateScore();

    void close();
}
